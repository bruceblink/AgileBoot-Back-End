package app.keystone.admin.customize.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.admin.customize.service.login.TokenService;
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

    private JwtAuthenticationTokenFilter filter;

    private FilterChain chain;

    @BeforeEach
    void setUp() {
        tokenService = Mockito.mock(TokenService.class);
        filter = new JwtAuthenticationTokenFilter(tokenService);
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
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldRethrowTokenExceptionWhenKeystoneTokenParsingFails() {
        MockHttpServletRequest request = requestWithBearer("invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiException original = new ApiException(ErrorCode.Client.INVALID_TOKEN);
        when(tokenService.getTokenFromRequest(request)).thenReturn("invalid-token");
        when(tokenService.getLoginUserByTokenSilently("invalid-token")).thenThrow(original);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
            .isSameAs(original);
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

    @Test
    void shouldSkipAuthenticationForRefreshTokenEndpoints() throws Exception {
        MockHttpServletRequest request = requestWithBearer("expired-access-token");
        request.setServletPath("/refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenService, never()).getLoginUserByTokenSilently(Mockito.anyString());
        verify(chain).doFilter(request, response);

        SecurityContextHolder.clearContext();
        MockHttpServletRequest logoutRequest = requestWithBearer("expired-access-token");
        logoutRequest.setServletPath("/logout-refresh-token");
        MockHttpServletResponse logoutResponse = new MockHttpServletResponse();

        filter.doFilter(logoutRequest, logoutResponse, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(logoutRequest, logoutResponse);
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
