package app.keystone.admin.customize.service.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.common.constant.Constants.Token;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void createTokenAndPutUserInCache_shouldIncludeStandardJwtClaims() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        when(loginAccountCache.setIfAbsent(eq("1"), anyString())).thenReturn(true);
        TokenService tokenService = new TokenService(redisCacheService);
        setField(tokenService, "secret", "0123456789abcdef0123456789abcdef");
        setField(tokenService, "expirationSeconds", 1800L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        String token = tokenService.createTokenAndPutUserInCache(loginUser);

        Claims claims = Jwts.parser()
            .verifyWith(signingKey("0123456789abcdef0123456789abcdef"))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertNotNull(claims.getId());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.get(Token.LOGIN_USER_KEY));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTokenAndPutUserInCache_shouldRejectWhenAccountHasActiveSession() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        TokenService tokenService = new TokenService(redisCacheService);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        SystemLoginUser existingLoginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        existingLoginUser.setCachedKey("existing-token-id");
        when(loginAccountCache.getObjectOnlyInCacheById("1")).thenReturn("existing-token-id");
        when(loginUserCache.getObjectOnlyInCacheById("existing-token-id")).thenReturn(existingLoginUser);

        assertThatThrownBy(() -> tokenService.createTokenAndPutUserInCache(loginUser))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(Business.LOGIN_ACCOUNT_ALREADY_LOGGED_IN);

        verify(loginUserCache).delete(loginUser.getCachedKey());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTokenAndPutUserInCache_shouldClearExpiredAccountMarkerAndLogin() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        TokenService tokenService = new TokenService(redisCacheService);
        setField(tokenService, "secret", "0123456789abcdef0123456789abcdef");
        setField(tokenService, "expirationSeconds", 1800L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        when(loginAccountCache.getObjectOnlyInCacheById("1")).thenReturn("expired-token-id");
        when(loginUserCache.getObjectOnlyInCacheById("expired-token-id")).thenReturn(null);
        when(loginAccountCache.setIfAbsent(eq("1"), anyString())).thenReturn(true);

        String token = tokenService.createTokenAndPutUserInCache(loginUser);

        assertThat(token).isNotBlank();
        verify(loginAccountCache).delete("1");
        verify(loginUserCache).set(loginUser.getCachedKey(), loginUser);
    }

    @Test
    @SuppressWarnings("unchecked")
    void removeLoginUser_shouldDeleteTokenAndMatchingAccountMarker() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        TokenService tokenService = new TokenService(redisCacheService);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        loginUser.setCachedKey("token-id");
        when(loginAccountCache.getObjectOnlyInCacheById("1")).thenReturn("token-id");

        tokenService.removeLoginUser(loginUser);

        verify(loginUserCache).delete("token-id");
        verify(loginAccountCache).delete("1");
    }

    private SecretKey signingKey(String secret) {
        byte[] keyBytes = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 64);
        return new SecretKeySpec(keyBytes, "HmacSHA512");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
