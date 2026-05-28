package app.keystone.admin.customize.service.login;

import app.keystone.common.constant.Constants.Token;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.infrastructure.user.web.LoginRefreshSession;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.AllArgsConstructor;
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

    @Value("${token.expirationSeconds:1800}")
    private long expirationSeconds;

    @Value("${token.refreshExpirationSeconds:604800}")
    private long refreshExpirationSeconds;

    @Value("${token.refreshSlidingExpirationEnabled:false}")
    private boolean refreshSlidingExpirationEnabled;

    @Value("${token.refreshLockSeconds:10}")
    private long refreshLockSeconds;

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
    public IssuedToken createTokenAndPutUserInCache(SystemLoginUser loginUser) {
        String tokenId = UUID.randomUUID().toString();
        String refreshSessionId = UUID.randomUUID().toString();
        String refreshTokenSecret = UUID.randomUUID() + UUID.randomUUID().toString();
        String refreshToken = formatRefreshToken(refreshSessionId, refreshTokenSecret);
        String accountId = loginAccountCacheId(loginUser);
        long issuedAt = System.currentTimeMillis();
        long expiresAt = issuedAt + TimeUnit.SECONDS.toMillis(refreshCacheTimeoutSeconds());

        loginUser.setCachedKey(tokenId);

        LoginRefreshSession refreshSession = new LoginRefreshSession();
        refreshSession.setRefreshSessionId(refreshSessionId);
        refreshSession.setRefreshTokenHash(hashRefreshToken(refreshTokenSecret));
        refreshSession.setAccountId(accountId);
        refreshSession.setUserId(loginUser.getUserId());
        refreshSession.setUsername(loginUser.getUsername());
        refreshSession.setCurrentTokenId(tokenId);
        refreshSession.setLoginUser(loginUser);
        refreshSession.setIssuedAt(issuedAt);
        refreshSession.setExpiresAt(expiresAt);
        refreshSession.setRevoked(false);

        try {
            occupyLoginAccount(accountId, refreshSessionId);
            redisCache.loginRefreshTokenCache.set(refreshSessionId, refreshSession, refreshCacheTimeoutSeconds(),
                TimeUnit.SECONDS);
            redisCache.loginUserCache.set(tokenId, loginUser, tokenCacheTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            redisCache.loginUserCache.delete(tokenId);
            redisCache.loginRefreshTokenCache.delete(refreshSessionId);
            releaseLoginAccount(accountId, refreshSessionId);
            throw e;
        }

        return new IssuedToken(generateAccessToken(loginUser, refreshSessionId), refreshToken, expirationSeconds,
            refreshExpirationSeconds);
    }

    public IssuedToken refreshAccessToken(String refreshToken) {
        ParsedRefreshToken parsedRefreshToken = parseRefreshToken(refreshToken);
        String lockValue = UUID.randomUUID().toString();
        acquireRefreshLock(parsedRefreshToken.refreshSessionId(), lockValue);
        try {
            LoginRefreshSession refreshSession = getValidRefreshSession(parsedRefreshToken);

            String currentAccountSessionId =
                redisCache.loginAccountCache.getObjectOnlyInRedisById(refreshSession.getAccountId());
            if (!refreshSession.getRefreshSessionId().equals(currentAccountSessionId)) {
                revokeRefreshSession(refreshSession);
                throw invalidRefreshToken();
            }

            String newTokenId = UUID.randomUUID().toString();
            String newRefreshTokenSecret = UUID.randomUUID() + UUID.randomUUID().toString();
            String newRefreshToken = formatRefreshToken(refreshSession.getRefreshSessionId(), newRefreshTokenSecret);
            long currentTimeMillis = System.currentTimeMillis();
            long refreshExpiresAt = refreshSlidingExpirationEnabled
                ? currentTimeMillis + TimeUnit.SECONDS.toMillis(refreshCacheTimeoutSeconds())
                : refreshSession.getExpiresAt();
            int refreshTtlSeconds = remainingRefreshTtlSeconds(refreshExpiresAt, currentTimeMillis);

            redisCache.loginUserCache.delete(refreshSession.getCurrentTokenId());

            SystemLoginUser loginUser = refreshSession.getLoginUser();
            loginUser.setCachedKey(newTokenId);
            refreshSession.setCurrentTokenId(newTokenId);
            refreshSession.setRefreshTokenHash(hashRefreshToken(newRefreshTokenSecret));
            refreshSession.setExpiresAt(refreshExpiresAt);
            refreshSession.setLoginUser(loginUser);

            redisCache.loginRefreshTokenCache.set(refreshSession.getRefreshSessionId(), refreshSession, refreshTtlSeconds,
                TimeUnit.SECONDS);
            redisCache.loginAccountCache.set(refreshSession.getAccountId(), refreshSession.getRefreshSessionId(),
                refreshTtlSeconds, TimeUnit.SECONDS);
            redisCache.loginUserCache.set(newTokenId, loginUser, tokenCacheTimeoutSeconds(), TimeUnit.SECONDS);

            return new IssuedToken(generateAccessToken(loginUser, refreshSession.getRefreshSessionId()), newRefreshToken,
                expirationSeconds, (long) refreshTtlSeconds);
        } finally {
            releaseRefreshLock(parsedRefreshToken.refreshSessionId(), lockValue);
        }
    }

    public void removeLoginUserByRefreshToken(String refreshToken) {
        ParsedRefreshToken parsedRefreshToken = parseRefreshToken(refreshToken);
        LoginRefreshSession refreshSession =
            redisCache.loginRefreshTokenCache.getObjectOnlyInRedisById(parsedRefreshToken.refreshSessionId());
        if (refreshSession == null) {
            return;
        }
        String tokenHash = hashRefreshToken(parsedRefreshToken.refreshTokenSecret());
        if (tokenHash.equals(refreshSession.getRefreshTokenHash())) {
            revokeRefreshSession(refreshSession);
        }
    }

    public void removeLoginUser(SystemLoginUser loginUser) {
        if (loginUser == null) {
            return;
        }
        if (!revokeRefreshSession(loginUser, loginUser.getCachedKey())) {
            redisCache.loginUserCache.delete(loginUser.getCachedKey());
        }
    }

    public SystemLoginUser removeLoginUserByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        Claims claims = parseToken(token);
        String cachedKey = (String) claims.get(Token.LOGIN_USER_KEY);
        if (cachedKey == null || cachedKey.isBlank()) {
            return null;
        }

        SystemLoginUser loginUser = redisCache.loginUserCache.getObjectOnlyInCacheById(cachedKey);
        String refreshSessionId = (String) claims.get(Token.LOGIN_REFRESH_SESSION_ID);

        if (loginUser != null) {
            if (!revokeRefreshSession(refreshSessionId, loginUser, cachedKey)) {
                redisCache.loginUserCache.delete(cachedKey);
            }
            return loginUser;
        }

        String accountId = loginAccountCacheId(claims);
        if (!revokeRefreshSession(refreshSessionId, accountId, cachedKey)) {
            redisCache.loginUserCache.delete(cachedKey);
        }
        return null;
    }

    public void removeLoginUser(String cachedKey) {
        SystemLoginUser loginUser = redisCache.loginUserCache.getObjectOnlyInCacheById(cachedKey);
        if (loginUser == null || !revokeRefreshSession(loginUser, cachedKey)) {
            redisCache.loginUserCache.delete(cachedKey);
        }
    }

    private void occupyLoginAccount(String accountId, String refreshSessionId) {
        String existingRefreshSessionId = redisCache.loginAccountCache.getObjectOnlyInRedisById(accountId);
        if (existingRefreshSessionId != null) {
            LoginRefreshSession existingRefreshSession =
                redisCache.loginRefreshTokenCache.getObjectOnlyInRedisById(existingRefreshSessionId);
            if (isRefreshSessionActive(existingRefreshSession)) {
                throw new ApiException(Business.LOGIN_ACCOUNT_ALREADY_LOGGED_IN);
            }
            redisCache.loginAccountCache.delete(accountId);
            if (existingRefreshSessionId != null) {
                redisCache.loginRefreshTokenCache.delete(existingRefreshSessionId);
            }
        }

        boolean occupied = redisCache.loginAccountCache.setIfAbsent(accountId, refreshSessionId,
            refreshCacheTimeoutSeconds(), TimeUnit.SECONDS);
        if (!occupied) {
            throw new ApiException(Business.LOGIN_ACCOUNT_ALREADY_LOGGED_IN);
        }
    }

    private boolean revokeRefreshSession(SystemLoginUser loginUser, String tokenId) {
        return revokeRefreshSession(null, loginUser, tokenId);
    }

    private boolean revokeRefreshSession(String refreshSessionId, SystemLoginUser loginUser, String tokenId) {
        String accountId = loginUser == null ? null : loginAccountCacheId(loginUser);
        return revokeRefreshSession(refreshSessionId, accountId, tokenId);
    }

    private boolean revokeRefreshSession(String refreshSessionId, String accountId, String tokenId) {
        LoginRefreshSession refreshSession = null;
        if (refreshSessionId != null && !refreshSessionId.isBlank()) {
            refreshSession = redisCache.loginRefreshTokenCache.getObjectOnlyInRedisById(refreshSessionId);
        }
        if (refreshSession == null && accountId != null && !accountId.isBlank()) {
            String currentRefreshSessionId = redisCache.loginAccountCache.getObjectOnlyInRedisById(accountId);
            if (currentRefreshSessionId != null) {
                LoginRefreshSession currentRefreshSession =
                    redisCache.loginRefreshTokenCache.getObjectOnlyInRedisById(currentRefreshSessionId);
                if (currentRefreshSession != null && tokenId != null
                    && tokenId.equals(currentRefreshSession.getCurrentTokenId())) {
                    refreshSession = currentRefreshSession;
                    refreshSessionId = currentRefreshSessionId;
                }
            }
        }
        if (refreshSession == null) {
            return false;
        }
        revokeRefreshSession(refreshSession);
        return true;
    }

    private void revokeRefreshSession(LoginRefreshSession refreshSession) {
        if (refreshSession == null) {
            return;
        }
        if (refreshSession.getCurrentTokenId() != null) {
            redisCache.loginUserCache.delete(refreshSession.getCurrentTokenId());
        }
        String currentRefreshSessionId = redisCache.loginAccountCache.getObjectOnlyInRedisById(refreshSession.getAccountId());
        if (refreshSession.getRefreshSessionId().equals(currentRefreshSessionId)) {
            redisCache.loginAccountCache.delete(refreshSession.getAccountId());
        }
        redisCache.loginRefreshTokenCache.delete(refreshSession.getRefreshSessionId());
    }

    private void releaseLoginAccount(String accountId, String refreshSessionId) {
        if (accountId == null || accountId.isBlank()) {
            return;
        }
        String currentRefreshSessionId = redisCache.loginAccountCache.getObjectOnlyInRedisById(accountId);
        if (refreshSessionId != null && refreshSessionId.equals(currentRefreshSessionId)) {
            redisCache.loginAccountCache.delete(accountId);
        }
    }

    private String loginAccountCacheId(SystemLoginUser loginUser) {
        if (loginUser.getUserId() != null) {
            return loginUser.getUserId().toString();
        }
        return loginUser.getUsername();
    }

    private String loginAccountCacheId(Claims claims) {
        Object userId = claims.get(Token.LOGIN_USER_ID);
        if (userId != null) {
            return userId.toString();
        }
        Object username = claims.get(Token.LOGIN_USERNAME);
        return username == null ? null : username.toString();
    }

    private int tokenCacheTimeoutSeconds() {
        if (expirationSeconds <= 0 || expirationSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("token.expirationSeconds must be between 1 and " + Integer.MAX_VALUE);
        }
        return Math.toIntExact(expirationSeconds);
    }

    private int refreshCacheTimeoutSeconds() {
        if (refreshExpirationSeconds <= 0 || refreshExpirationSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("token.refreshExpirationSeconds must be between 1 and "
                + Integer.MAX_VALUE);
        }
        return Math.toIntExact(refreshExpirationSeconds);
    }

    private int remainingRefreshTtlSeconds(long expiresAt, long currentTimeMillis) {
        long remainingMillis = expiresAt - currentTimeMillis;
        if (remainingMillis <= 0) {
            throw invalidRefreshToken();
        }
        long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis);
        if (remainingSeconds <= 0) {
            remainingSeconds = 1;
        }
        if (remainingSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("refresh session ttl must be no greater than " + Integer.MAX_VALUE);
        }
        return Math.toIntExact(remainingSeconds);
    }

    private boolean isRefreshSessionActive(LoginRefreshSession refreshSession) {
        return refreshSession != null && !refreshSession.isRevoked() && !refreshSession.isExpired(System.currentTimeMillis());
    }

    private void acquireRefreshLock(String refreshSessionId, String lockValue) {
        boolean locked = redisCache.loginRefreshLockCache.setIfAbsent(refreshSessionId, lockValue,
            refreshLockTimeoutSeconds(), TimeUnit.SECONDS);
        if (!locked) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_TOO_OFTEN);
        }
    }

    private void releaseRefreshLock(String refreshSessionId, String lockValue) {
        String currentLockValue = redisCache.loginRefreshLockCache.getObjectOnlyInRedisById(refreshSessionId);
        if (lockValue.equals(currentLockValue)) {
            redisCache.loginRefreshLockCache.delete(refreshSessionId);
        }
    }

    private int refreshLockTimeoutSeconds() {
        if (refreshLockSeconds <= 0 || refreshLockSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("token.refreshLockSeconds must be between 1 and " + Integer.MAX_VALUE);
        }
        return Math.toIntExact(refreshLockSeconds);
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
    private String generateAccessToken(SystemLoginUser loginUser, String refreshSessionId) {
        return generateToken(Map.of(
            Token.LOGIN_USER_KEY, loginUser.getCachedKey(),
            Token.LOGIN_USER_ID, loginUser.getUserId(),
            Token.LOGIN_USERNAME, loginUser.getUsername(),
            Token.LOGIN_REFRESH_SESSION_ID, refreshSessionId
        ));
    }

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

    private ParsedRefreshToken parseRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidRefreshToken();
        }
        String[] parts = refreshToken.split("\\.", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw invalidRefreshToken();
        }
        return new ParsedRefreshToken(parts[0], parts[1]);
    }

    private LoginRefreshSession getValidRefreshSession(ParsedRefreshToken parsedRefreshToken) {
        LoginRefreshSession refreshSession =
            redisCache.loginRefreshTokenCache.getObjectOnlyInRedisById(parsedRefreshToken.refreshSessionId());
        if (!isRefreshSessionActive(refreshSession)) {
            throw invalidRefreshToken();
        }
        String tokenHash = hashRefreshToken(parsedRefreshToken.refreshTokenSecret());
        if (!tokenHash.equals(refreshSession.getRefreshTokenHash())) {
            revokeRefreshSession(refreshSession);
            throw invalidRefreshToken();
        }
        if (refreshSession.getLoginUser() == null) {
            revokeRefreshSession(refreshSession);
            throw invalidRefreshToken();
        }
        return refreshSession;
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(ErrorCode.Client.INVALID_TOKEN);
    }

    private String formatRefreshToken(String refreshSessionId, String refreshTokenSecret) {
        return refreshSessionId + "." + refreshTokenSecret;
    }

    private String hashRefreshToken(String refreshTokenSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshTokenSecret.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Internal.INTERNAL_ERROR, e.getMessage());
        }
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

    @Data
    @AllArgsConstructor
    public static class IssuedToken {

        private String token;

        private String refreshToken;

        private Long expiresIn;

        private Long refreshExpiresIn;
    }

    private record ParsedRefreshToken(String refreshSessionId, String refreshTokenSecret) {
    }

}
