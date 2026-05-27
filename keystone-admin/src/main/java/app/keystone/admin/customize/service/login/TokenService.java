package app.keystone.admin.customize.service.login;

import app.keystone.common.constant.Constants.Token;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * token验证处理
 *
 * @author valarchie
 */
@Component
@Slf4j
@Data
@RequiredArgsConstructor
public class TokenService {

    /**
     * 自定义令牌标识
     */
    @Value("${token.header}")
    private String header;

    /**
     * 令牌秘钥
     */
    @Value("${token.secret}")
    private String secret;

    /**
     * 自动刷新token的时间，当过期时间不足autoRefreshTime的值的时候，会触发刷新用户登录缓存的时间
     * 比如这个值是20,   用户是8点登录的， 8点半缓存会过期， 当过8.10分的时候，就少于20分钟了，便触发
     * 刷新登录用户的缓存时间
     */
    @Value("${token.autoRefreshTime}")
    private long autoRefreshTime;

    @Value("${token.expirationSeconds:1800}")
    private long expirationSeconds;

    private final RedisCacheService redisCache;

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public SystemLoginUser getLoginUser(HttpServletRequest request) {
        // 获取请求携带的令牌
        String token = getTokenFromRequest(request);
        return getLoginUserByToken(token);
    }

    public SystemLoginUser getLoginUserByToken(String token) {
        return getLoginUserByToken(token, true);
    }

    public SystemLoginUser getLoginUserByTokenSilently(String token) {
        return getLoginUserByToken(token, false);
    }

    private SystemLoginUser getLoginUserByToken(String token, boolean logInvalidToken) {
        if (token != null && !token.isEmpty()) {
            try {
                Claims claims = parseToken(token);
                // 解析对应的权限以及用户信息
                String uuid = (String) claims.get(Token.LOGIN_USER_KEY);

                return redisCache.loginUserCache.getObjectOnlyInCacheById(uuid);
            } catch (ExpiredJwtException | SignatureException | MalformedJwtException | UnsupportedJwtException
                | IllegalArgumentException jwtException) {
                if (logInvalidToken) {
                    log.error("parse token failed.", jwtException);
                }
                throw new ApiException(jwtException, ErrorCode.Client.INVALID_TOKEN);
            } catch (Exception e) {
                log.error("fail to get cached user from redis", e);
                throw new ApiException(e, ErrorCode.Client.TOKEN_PROCESS_FAILED, e.getMessage());
            }

        }
        return null;
    }

    /**
     * 创建令牌
     *
     * @param loginUser 用户信息
     * @return 令牌
     */
    public String createTokenAndPutUserInCache(SystemLoginUser loginUser) {
        loginUser.setCachedKey(UUID.randomUUID().toString());

        redisCache.loginUserCache.set(loginUser.getCachedKey(), loginUser);
        try {
            occupyLoginAccount(loginUser);
        } catch (RuntimeException e) {
            redisCache.loginUserCache.delete(loginUser.getCachedKey());
            throw e;
        }

        return generateToken(Map.of(Token.LOGIN_USER_KEY, loginUser.getCachedKey()));
    }

    /**
     * 当超过20分钟，自动刷新token
     * @param loginUser 登录用户
     */
    public void refreshToken(SystemLoginUser loginUser) {
        long currentTime = System.currentTimeMillis();
        if (currentTime > loginUser.getAutoRefreshCacheTime()) {
            loginUser.setAutoRefreshCacheTime(currentTime + TimeUnit.MINUTES.toMillis(autoRefreshTime));
            // 根据uuid将loginUser存入缓存
            redisCache.loginUserCache.set(loginUser.getCachedKey(), loginUser);
            redisCache.loginAccountCache.set(loginAccountCacheId(loginUser), loginUser.getCachedKey());
        }
    }

    public void removeLoginUser(SystemLoginUser loginUser) {
        if (loginUser == null) {
            return;
        }
        redisCache.loginUserCache.delete(loginUser.getCachedKey());
        releaseLoginAccount(loginUser, loginUser.getCachedKey());
    }

    public void removeLoginUser(String cachedKey) {
        SystemLoginUser loginUser = redisCache.loginUserCache.getObjectOnlyInCacheById(cachedKey);
        redisCache.loginUserCache.delete(cachedKey);
        if (loginUser != null) {
            releaseLoginAccount(loginUser, cachedKey);
        }
    }

    private void occupyLoginAccount(SystemLoginUser loginUser) {
        String accountId = loginAccountCacheId(loginUser);
        String existingCachedKey = redisCache.loginAccountCache.getObjectOnlyInCacheById(accountId);
        if (existingCachedKey != null) {
            SystemLoginUser existingLoginUser = redisCache.loginUserCache.getObjectOnlyInCacheById(existingCachedKey);
            if (existingLoginUser != null) {
                throw new ApiException(Business.LOGIN_ACCOUNT_ALREADY_LOGGED_IN);
            }
            redisCache.loginAccountCache.delete(accountId);
        }

        boolean occupied = redisCache.loginAccountCache.setIfAbsent(accountId, loginUser.getCachedKey());
        if (!occupied) {
            throw new ApiException(Business.LOGIN_ACCOUNT_ALREADY_LOGGED_IN);
        }
    }

    private void releaseLoginAccount(SystemLoginUser loginUser, String cachedKey) {
        String accountId = loginAccountCacheId(loginUser);
        String currentCachedKey = redisCache.loginAccountCache.getObjectOnlyInCacheById(accountId);
        if (cachedKey != null && cachedKey.equals(currentCachedKey)) {
            redisCache.loginAccountCache.delete(accountId);
        }
    }

    private String loginAccountCacheId(SystemLoginUser loginUser) {
        if (loginUser.getUserId() != null) {
            return loginUser.getUserId().toString();
        }
        return loginUser.getUsername();
    }


    private SecretKey getSigningKey() {
        byte[] keyBytes = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 64);
        return new SecretKeySpec(keyBytes, "HmacSHA512");
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    private String generateToken(Map<String, Object> claims) {
        long currentTimeMillis = System.currentTimeMillis();
        Date issuedAt = new Date(currentTimeMillis);
        Date expiresAt = new Date(currentTimeMillis + TimeUnit.SECONDS.toMillis(expirationSeconds));
        return Jwts.builder()
            .claims(claims)
            .id(UUID.randomUUID().toString())
            .issuedAt(issuedAt)
            .expiration(expiresAt)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * 获取请求token
     *
     * @return token
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader(header);
        if (token != null && !token.isEmpty() && token.regionMatches(true, 0, Token.PREFIX, 0, Token.PREFIX.length())) {
            token = token.substring(Token.PREFIX.length());
        }
        return token;
    }

}
