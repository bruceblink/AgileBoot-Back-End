package app.keystone.admin.customize.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.admin.customize.service.login.TokenService;
import app.keystone.admin.customize.service.login.keylo.KeyloProperties;
import app.keystone.admin.customize.service.login.keylo.KeyloTokenIdentity;
import app.keystone.admin.customize.service.login.keylo.KeyloTokenVerifier;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationTokenFilterTest {

    private TokenService tokenService;

    private KeyloProperties keyloProperties;

    private KeyloTokenVerifier keyloTokenVerifier;

    private JwtAuthenticationTokenFilter filter;

    private FilterChain chain;

    @BeforeEach
    void setUp() {
        tokenService = Mockito.mock(TokenService.class);
        keyloProperties = Mockito.mock(KeyloProperties.class);
        keyloTokenVerifier = Mockito.mock(KeyloTokenVerifier.class);
        filter = new JwtAuthenticationTokenFilter(tokenService, keyloProperties, keyloTokenVerifier);
        chain = Mockito.mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUseKeystoneTokenWhenItCanBeResolved() throws Exception {
        MockHttpServletRequest request = requestWithBearer("keystone-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SystemLoginUser loginUser = loginUser(1L, "admin");
        loginUser.setCachedKey("cache-key");
        when(tokenService.getTokenFromRequest(request)).thenReturn("keystone-token");
        when(tokenService.getLoginUserByTokenSilently("keystone-token")).thenReturn(loginUser);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(loginUser);
        verify(keyloTokenVerifier, never()).verify(Mockito.anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldFallbackToKeyloAccessTokenWhenKeystoneTokenParsingFails() throws Exception {
        MockHttpServletRequest request = requestWithBearer("keylo-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.getTokenFromRequest(request)).thenReturn("keylo-access-token");
        when(tokenService.getLoginUserByTokenSilently("keylo-access-token"))
            .thenThrow(new ApiException(ErrorCode.Client.INVALID_TOKEN));
        when(keyloProperties.isEnabled()).thenReturn(true);
        when(keyloTokenVerifier.verify("keylo-access-token"))
            .thenReturn(new KeyloTokenIdentity("service:sys_test", null, "keylo-access-token", null, null, "access"));

        filter.doFilter(request, response, chain);

        SystemLoginUser loginUser = (SystemLoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(loginUser.getUsername()).isEqualTo("service:sys_test");
        assertThat(loginUser.isAdmin()).isTrue();
        assertThat(loginUser.getRoleInfo().getMenuPermissions()).contains(RoleInfo.ALL_PERMISSIONS);
        verify(keyloTokenVerifier).verify("keylo-access-token");
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldRethrowKeyloTokenExceptionWhenKeyloFallbackFails() {
        MockHttpServletRequest request = requestWithBearer("invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiException original = new ApiException(ErrorCode.Client.INVALID_TOKEN);
        ApiException keyloException = new ApiException(ErrorCode.Client.INVALID_TOKEN);
        when(tokenService.getTokenFromRequest(request)).thenReturn("invalid-token");
        when(tokenService.getLoginUserByTokenSilently("invalid-token")).thenThrow(original);
        when(keyloProperties.isEnabled()).thenReturn(true);
        when(keyloTokenVerifier.verify("invalid-token")).thenThrow(keyloException);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
            .isSameAs(keyloException);
    }

    @Test
    void shouldSkipAuthenticationWhenTokenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.getTokenFromRequest(request)).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenService, never()).getLoginUserByTokenSilently(Mockito.anyString());
        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private SystemLoginUser loginUser(Long userId, String username) {
        return new SystemLoginUser(userId, false, username, "pwd", RoleInfo.EMPTY_ROLE, 1L);
    }
}
