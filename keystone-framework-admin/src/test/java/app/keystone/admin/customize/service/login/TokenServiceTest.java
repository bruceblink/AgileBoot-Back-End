package app.keystone.admin.customize.service.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.common.config.KeystoneConfig;
import app.keystone.common.constant.Constants.Token;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.user.web.LoginRefreshSession;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TokenServiceTest {

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = generateKeyPair();
        KeystoneConfig keystoneConfig = new KeystoneConfig();
        keystoneConfig.setRsaPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        setKeystoneConfig(keystoneConfig);
    }

    @AfterEach
    void tearDown() throws Exception {
        setKeystoneConfig(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTokenAndPutUserInCache_shouldIncludeStandardJwtClaims() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginRefreshLockCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginRefreshTokenCache = loginRefreshTokenCache;
        redisCacheService.loginRefreshLockCache = loginRefreshLockCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        when(loginAccountCache.setIfAbsent(eq("1"), anyString(), eq(604800), eq(TimeUnit.SECONDS))).thenReturn(true);
        TokenService tokenService = new TokenService(redisCacheService, new KeystoneRsaKeyService());
        setField(tokenService, "expirationSeconds", 1800L);
        setField(tokenService, "refreshExpirationSeconds", 604800L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        TokenService.IssuedToken issuedToken = tokenService.createTokenAndPutUserInCache(loginUser);

        verify(loginUserCache).set(loginUser.getCachedKey(), loginUser, 1800, TimeUnit.SECONDS);
        verify(loginRefreshTokenCache).set(anyString(), org.mockito.ArgumentMatchers.any(LoginRefreshSession.class),
            eq(604800), eq(TimeUnit.SECONDS));
        verify(loginAccountCache).setIfAbsent(eq("1"), anyString(), eq(604800), eq(TimeUnit.SECONDS));

        var jws = Jwts.parser()
            .verifyWith(keyPair.getPublic())
            .build()
            .parseSignedClaims(issuedToken.getToken());
        Claims claims = jws.getPayload();

        assertNotNull(claims.getId());
        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("RS256");
        assertThat(claims.getIssuer()).isEqualTo("keystone");
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.get(Token.LOGIN_USER_KEY));
        assertThat(claims.get(Token.LOGIN_USER_ID, Long.class)).isEqualTo(1L);
        assertThat(claims.get(Token.LOGIN_USERNAME, String.class)).isEqualTo("admin");
        assertThat(claims.get(Token.LOGIN_REFRESH_SESSION_ID, String.class)).isNotBlank();
        assertThat(issuedToken.getRefreshToken()).contains(".");
        assertThat(issuedToken.getExpiresIn()).isEqualTo(1800L);
        assertThat(issuedToken.getRefreshExpiresIn()).isEqualTo(604800L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTokenAndPutUserInCache_shouldRejectWhenAccountHasActiveSession() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginRefreshLockCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginRefreshTokenCache = loginRefreshTokenCache;
        redisCacheService.loginRefreshLockCache = loginRefreshLockCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        TokenService tokenService = new TokenService(redisCacheService, new KeystoneRsaKeyService());
        setField(tokenService, "expirationSeconds", 1800L);
        setField(tokenService, "refreshExpirationSeconds", 604800L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        SystemLoginUser existingLoginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        LoginRefreshSession existingRefreshSession = new LoginRefreshSession();
        existingRefreshSession.setRefreshSessionId("existing-refresh-session-id");
        existingRefreshSession.setAccountId("1");
        existingRefreshSession.setCurrentTokenId("existing-token-id");
        existingRefreshSession.setExpiresAt(System.currentTimeMillis() + 60_000);
        when(loginAccountCache.getFromRedis("1")).thenReturn("existing-refresh-session-id");
        when(loginRefreshTokenCache.getFromRedis("existing-refresh-session-id"))
            .thenReturn(existingRefreshSession);
        when(loginUserCache.getFromRedis("existing-token-id")).thenReturn(existingLoginUser);

        assertThatThrownBy(() -> tokenService.createTokenAndPutUserInCache(loginUser))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(Business.LOGIN_ACCOUNT_ALREADY_LOGGED_IN);

        verify(loginUserCache).delete(loginUser.getCachedKey());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTokenAndPutUserInCache_shouldRevokeExistingSessionWhenForceLogin() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginRefreshLockCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginRefreshTokenCache = loginRefreshTokenCache;
        redisCacheService.loginRefreshLockCache = loginRefreshLockCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        when(loginAccountCache.setIfAbsent(eq("1"), anyString(), eq(604800), eq(TimeUnit.SECONDS))).thenReturn(true);
        TokenService tokenService = new TokenService(redisCacheService, new KeystoneRsaKeyService());
        setField(tokenService, "expirationSeconds", 1800L);
        setField(tokenService, "refreshExpirationSeconds", 604800L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        SystemLoginUser existingLoginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        existingLoginUser.setCachedKey("existing-token-id");
        LoginRefreshSession existingRefreshSession = refreshSession(
            "existing-refresh-session-id", "1", "existing-token-id", existingLoginUser);
        when(loginAccountCache.getFromRedis("1"))
            .thenReturn("existing-refresh-session-id")
            .thenReturn("existing-refresh-session-id");
        when(loginRefreshTokenCache.getFromRedis("existing-refresh-session-id"))
            .thenReturn(existingRefreshSession);
        when(loginUserCache.getFromRedis("existing-token-id")).thenReturn(existingLoginUser);

        TokenService.IssuedToken issuedToken = tokenService.createTokenAndPutUserInCache(loginUser, true);

        assertThat(issuedToken.getToken()).isNotBlank();
        verify(loginUserCache).delete("existing-token-id");
        verify(loginAccountCache).delete("1");
        verify(loginRefreshTokenCache).delete("existing-refresh-session-id");
        verify(loginUserCache).set(loginUser.getCachedKey(), loginUser, 1800, TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTokenAndPutUserInCache_shouldClearRefreshSessionWhenAccessSessionIsMissing() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginRefreshLockCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginRefreshTokenCache = loginRefreshTokenCache;
        redisCacheService.loginRefreshLockCache = loginRefreshLockCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        TokenService tokenService = new TokenService(redisCacheService, new KeystoneRsaKeyService());
        setField(tokenService, "expirationSeconds", 1800L);
        setField(tokenService, "refreshExpirationSeconds", 604800L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "testk", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        LoginRefreshSession existingRefreshSession = refreshSession(
            "existing-refresh-session-id", "1", "missing-token-id", loginUser);
        when(loginAccountCache.getFromRedis("1"))
            .thenReturn("existing-refresh-session-id")
            .thenReturn("existing-refresh-session-id");
        when(loginRefreshTokenCache.getFromRedis("existing-refresh-session-id"))
            .thenReturn(existingRefreshSession);
        when(loginUserCache.getFromRedis("missing-token-id")).thenReturn(null);
        when(loginAccountCache.setIfAbsent(eq("1"), anyString(), eq(604800), eq(TimeUnit.SECONDS))).thenReturn(true);

        TokenService.IssuedToken issuedToken = tokenService.createTokenAndPutUserInCache(loginUser);

        assertThat(issuedToken.getToken()).isNotBlank();
        verify(loginUserCache).delete("missing-token-id");
        verify(loginAccountCache).delete("1");
        verify(loginRefreshTokenCache).delete("existing-refresh-session-id");
        verify(loginUserCache).set(loginUser.getCachedKey(), loginUser, 1800, TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTokenAndPutUserInCache_shouldClearExpiredAccountMarkerAndLogin() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginRefreshLockCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginRefreshTokenCache = loginRefreshTokenCache;
        redisCacheService.loginRefreshLockCache = loginRefreshLockCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        TokenService tokenService = new TokenService(redisCacheService, new KeystoneRsaKeyService());
        setField(tokenService, "expirationSeconds", 1800L);
        setField(tokenService, "refreshExpirationSeconds", 604800L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        when(loginAccountCache.getFromRedis("1")).thenReturn("expired-refresh-session-id");
        when(loginRefreshTokenCache.getFromRedis("expired-refresh-session-id")).thenReturn(null);
        when(loginAccountCache.setIfAbsent(eq("1"), anyString(), eq(604800), eq(TimeUnit.SECONDS))).thenReturn(true);

        TokenService.IssuedToken issuedToken = tokenService.createTokenAndPutUserInCache(loginUser);

        assertThat(issuedToken.getToken()).isNotBlank();
        verify(loginAccountCache).delete("1");
        verify(loginRefreshTokenCache).delete("expired-refresh-session-id");
        verify(loginUserCache).set(loginUser.getCachedKey(), loginUser, 1800, TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void removeLoginUser_shouldDeleteTokenAndMatchingAccountMarker() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginRefreshLockCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginRefreshTokenCache = loginRefreshTokenCache;
        redisCacheService.loginRefreshLockCache = loginRefreshLockCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        TokenService tokenService = new TokenService(redisCacheService, new KeystoneRsaKeyService());

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        loginUser.setCachedKey("token-id");
        LoginRefreshSession refreshSession = refreshSession("refresh-session-id", "1", "token-id", loginUser);
        when(loginAccountCache.getFromRedis("1")).thenReturn("refresh-session-id");
        when(loginRefreshTokenCache.getFromRedis("refresh-session-id")).thenReturn(refreshSession);

        tokenService.removeLoginUser(loginUser);

        verify(loginUserCache).delete("token-id");
        verify(loginAccountCache).delete("1");
        verify(loginRefreshTokenCache).delete("refresh-session-id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void removeLoginUserByToken_shouldReleaseAccountMarkerWhenLoginUserCacheExists() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginRefreshLockCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginRefreshTokenCache = loginRefreshTokenCache;
        redisCacheService.loginRefreshLockCache = loginRefreshLockCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        TokenService tokenService = new TokenService(redisCacheService, new KeystoneRsaKeyService());
        setField(tokenService, "expirationSeconds", 1800L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        loginUser.setCachedKey("token-id");
        String token = generateToken(tokenService, "token-id", "refresh-session-id", 1L, "admin");
        LoginRefreshSession refreshSession = refreshSession("refresh-session-id", "1", "token-id", loginUser);
        when(loginUserCache.getFromRedis("token-id")).thenReturn(loginUser);
        when(loginRefreshTokenCache.getFromRedis("refresh-session-id")).thenReturn(refreshSession);
        when(loginAccountCache.getFromRedis("1")).thenReturn("refresh-session-id");

        SystemLoginUser removedLoginUser = tokenService.removeLoginUserByToken(token);

        assertThat(removedLoginUser).isEqualTo(loginUser);
        verify(loginUserCache).delete("token-id");
        verify(loginAccountCache).delete("1");
        verify(loginRefreshTokenCache).delete("refresh-session-id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void removeLoginUserByToken_shouldReleaseAccountMarkerByJwtClaimsWhenLoginUserCacheIsMissing() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginRefreshLockCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginRefreshTokenCache = loginRefreshTokenCache;
        redisCacheService.loginRefreshLockCache = loginRefreshLockCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        TokenService tokenService = new TokenService(redisCacheService, new KeystoneRsaKeyService());
        setField(tokenService, "expirationSeconds", 1800L);

        String token = generateToken(tokenService, "token-id", "refresh-session-id", 1L, "admin");
        LoginRefreshSession refreshSession = refreshSession("refresh-session-id", "1", "token-id", null);
        when(loginUserCache.getFromRedis("token-id")).thenReturn(null);
        when(loginRefreshTokenCache.getFromRedis("refresh-session-id")).thenReturn(refreshSession);
        when(loginAccountCache.getFromRedis("1")).thenReturn("refresh-session-id");

        SystemLoginUser removedLoginUser = tokenService.removeLoginUserByToken(token);

        assertThat(removedLoginUser).isNull();
        verify(loginUserCache).delete("token-id");
        verify(loginAccountCache).delete("1");
        verify(loginRefreshTokenCache).delete("refresh-session-id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshAccessToken_shouldRotateAccessAndRefreshTokenWithoutExtendingFixedRefreshSession() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginRefreshLockCache = mock(RedisCacheTemplate.class);
        RedisCacheTemplate<String> loginAccountCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        redisCacheService.loginRefreshTokenCache = loginRefreshTokenCache;
        redisCacheService.loginRefreshLockCache = loginRefreshLockCache;
        redisCacheService.loginAccountCache = loginAccountCache;
        when(loginAccountCache.setIfAbsent(eq("1"), anyString(), eq(604800), eq(TimeUnit.SECONDS))).thenReturn(true);
        TokenService tokenService = new TokenService(redisCacheService, new KeystoneRsaKeyService());
        setField(tokenService, "expirationSeconds", 1800L);
        setField(tokenService, "refreshExpirationSeconds", 604800L);
        setField(tokenService, "refreshSlidingExpirationEnabled", false);
        setField(tokenService, "refreshLockSeconds", 10L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        TokenService.IssuedToken issuedToken = tokenService.createTokenAndPutUserInCache(loginUser);
        String refreshSessionId = issuedToken.getRefreshToken().split("\\.", 2)[0];

        ArgumentCaptor<LoginRefreshSession> refreshSessionCaptor = ArgumentCaptor.forClass(LoginRefreshSession.class);
        verify(loginRefreshTokenCache).set(eq(refreshSessionId), refreshSessionCaptor.capture(), eq(604800),
            eq(TimeUnit.SECONDS));
        LoginRefreshSession refreshSession = refreshSessionCaptor.getValue();
        when(loginRefreshTokenCache.getFromRedis(refreshSessionId)).thenReturn(refreshSession);
        when(loginAccountCache.getFromRedis("1")).thenReturn(refreshSessionId);
        org.mockito.Mockito.doAnswer(invocation -> {
            when(loginRefreshLockCache.getFromRedis(refreshSessionId)).thenReturn(invocation.getArgument(1));
            return true;
        }).when(loginRefreshLockCache).setIfAbsent(eq(refreshSessionId), anyString(), eq(10), eq(TimeUnit.SECONDS));

        String oldTokenId = loginUser.getCachedKey();

        TokenService.IssuedToken refreshedToken = tokenService.refreshAccessToken(issuedToken.getRefreshToken());

        assertThat(refreshedToken.getToken()).isNotBlank();
        assertThat(refreshedToken.getRefreshToken()).isNotEqualTo(issuedToken.getRefreshToken());
        assertThat(refreshedToken.getExpiresIn()).isEqualTo(1800L);
        assertThat(refreshedToken.getRefreshExpiresIn()).isBetween(604790L, 604800L);
        verify(loginUserCache).delete(oldTokenId);
        verify(loginAccountCache).set(eq("1"), eq(refreshSessionId), org.mockito.ArgumentMatchers.intThat(ttl -> ttl > 604790 && ttl <= 604800),
            eq(TimeUnit.SECONDS));
        verify(loginRefreshLockCache).delete(refreshSessionId);
    }

    private String generateToken(TokenService tokenService, String cachedKey, String refreshSessionId, Long userId,
        String username) throws Exception {
        java.lang.reflect.Method method = TokenService.class.getDeclaredMethod("generateToken", java.util.Map.class);
        method.setAccessible(true);
        return (String) method.invoke(tokenService, java.util.Map.of(
            Token.LOGIN_USER_KEY, cachedKey,
            Token.LOGIN_REFRESH_SESSION_ID, refreshSessionId,
            Token.LOGIN_USER_ID, userId,
            Token.LOGIN_USERNAME, username
        ));
    }

    private LoginRefreshSession refreshSession(String refreshSessionId, String accountId, String tokenId,
        SystemLoginUser loginUser) {
        LoginRefreshSession refreshSession = new LoginRefreshSession();
        refreshSession.setRefreshSessionId(refreshSessionId);
        refreshSession.setAccountId(accountId);
        refreshSession.setCurrentTokenId(tokenId);
        refreshSession.setLoginUser(loginUser);
        refreshSession.setExpiresAt(System.currentTimeMillis() + 60_000);
        return refreshSession;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private void setKeystoneConfig(KeystoneConfig keystoneConfig) throws Exception {
        Field instanceField = KeystoneConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, keystoneConfig);
    }
}
