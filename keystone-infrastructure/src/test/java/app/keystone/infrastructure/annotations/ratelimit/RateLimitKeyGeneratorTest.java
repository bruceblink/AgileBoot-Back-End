package app.keystone.infrastructure.annotations.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit.LimitType;
import app.keystone.infrastructure.user.app.AppLoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RateLimitKeyGeneratorTest {

    @AfterEach
    void cleanContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void generate_shouldCreateSeparatedGlobalKey() {
        RateLimit rateLimit = rateLimit("Test:", LimitType.GLOBAL);

        assertEquals("Test:GLOBAL:GLOBAL", RateLimitKeyGenerator.generate(rateLimit));
    }

    @Test
    void generate_shouldEscapeSegmentsBeforeJoiningKey() {
        RateLimit rateLimit = rateLimit("Rate-Limit:Login\\Captcha:", LimitType.GLOBAL);

        assertEquals("Rate-Limit\\:Login\\\\Captcha:GLOBAL:GLOBAL", RateLimitKeyGenerator.generate(rateLimit));
    }

    @Test
    void generate_shouldUseRequestIpForIpLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RateLimit rateLimit = rateLimit("Test:", LimitType.IP);

        assertEquals("Test:IP:10.0.0.1", RateLimitKeyGenerator.generate(rateLimit));
    }

    @Test
    void generate_shouldRejectIpLimitWithoutRequestContext() {
        RateLimit rateLimit = rateLimit("Test:", LimitType.IP);

        ApiException exception = assertThrows(ApiException.class, () -> RateLimitKeyGenerator.generate(rateLimit));

        assertEquals(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, exception.getErrorCode());
    }

    @Test
    void generate_shouldUseAppUserIdWhenUsernameIsEmpty() {
        AppLoginUser appLoginUser = new AppLoginUser(42L, true, "cache-key");
        authenticate(appLoginUser);

        RateLimit rateLimit = rateLimit("App:", LimitType.APP_USER);

        assertEquals("App:APP_USER:USER\\:42", RateLimitKeyGenerator.generate(rateLimit));
    }

    @Test
    void generate_shouldRejectUnauthenticatedUserPrincipal() {
        AppLoginUser appLoginUser = new AppLoginUser(42L, true, "cache-key");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(appLoginUser, null);
        authentication.setAuthenticated(false);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        RateLimit rateLimit = rateLimit("App:", LimitType.APP_USER);

        ApiException exception = assertThrows(ApiException.class, () -> RateLimitKeyGenerator.generate(rateLimit));

        assertEquals(ErrorCode.Client.COMMON_NO_AUTHORIZATION, exception.getErrorCode());
    }

    @Test
    void generate_shouldRejectMissingUserAuthentication() {
        RateLimit rateLimit = rateLimit("App:", LimitType.APP_USER);

        ApiException exception = assertThrows(ApiException.class, () -> RateLimitKeyGenerator.generate(rateLimit));

        assertEquals(ErrorCode.Client.COMMON_NO_AUTHORIZATION, exception.getErrorCode());
    }

    private static RateLimit rateLimit(String key, LimitType limitType) {
        RateLimit rateLimit = mock(RateLimit.class);
        when(rateLimit.key()).thenReturn(key);
        when(rateLimit.limitType()).thenReturn(limitType);
        return rateLimit;
    }

    private static void authenticate(Object principal) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(principal, null, "ROLE_USER"));
        SecurityContextHolder.setContext(context);
    }
}
