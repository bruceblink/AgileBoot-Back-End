package app.keystone.infrastructure.annotations.ratelimit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.annotations.ratelimit.implementation.LocalRateLimitChecker;
import app.keystone.infrastructure.annotations.ratelimit.implementation.RedisRateLimitChecker;
import org.junit.jupiter.api.Test;

class RateLimitCheckerTest {

    private final RedisRateLimitChecker redisRateLimitChecker = mock(RedisRateLimitChecker.class);

    private final LocalRateLimitChecker localRateLimitChecker = mock(LocalRateLimitChecker.class);

    private final RateLimitProperties properties = new RateLimitProperties();

    private final RateLimitChecker checker =
        new RateLimitChecker(redisRateLimitChecker, localRateLimitChecker, properties);

    private final RateLimit rateLimit = mock(RateLimit.class);

    @Test
    void check_shouldUseRedisByDefault() {
        checker.check(rateLimit);

        verify(redisRateLimitChecker).check(rateLimit);
        verify(localRateLimitChecker, never()).check(rateLimit);
    }

    @Test
    void check_shouldUseLocalBackendWhenConfigured() {
        properties.setBackend(RateLimitBackend.LOCAL);

        checker.check(rateLimit);

        verify(localRateLimitChecker).check(rateLimit);
        verify(redisRateLimitChecker, never()).check(rateLimit);
    }

    @Test
    void check_shouldNotFallbackWhenRedisFailsByDefault() {
        ApiException redisFailure = new ApiException(ErrorCode.Internal.GET_CACHE_FAILED);
        org.mockito.Mockito.doThrow(redisFailure).when(redisRateLimitChecker).check(rateLimit);

        ApiException exception = assertThrows(ApiException.class, () -> checker.check(rateLimit));

        assertSame(redisFailure, exception);
        verify(localRateLimitChecker, never()).check(rateLimit);
    }

    @Test
    void check_shouldFallbackToLocalWhenRedisCacheFailsAndFallbackEnabled() {
        properties.setFallbackToLocal(true);
        org.mockito.Mockito.doThrow(new ApiException(ErrorCode.Internal.GET_CACHE_FAILED))
            .when(redisRateLimitChecker).check(rateLimit);
        when(rateLimit.key()).thenReturn("Rate-Limit:Test:");

        checker.check(rateLimit);

        verify(redisRateLimitChecker).check(rateLimit);
        verify(localRateLimitChecker).check(rateLimit);
    }

    @Test
    void check_shouldNotFallbackForClientLimitFailures() {
        properties.setFallbackToLocal(true);
        ApiException tooOften = new ApiException(ErrorCode.Client.COMMON_REQUEST_TOO_OFTEN);
        org.mockito.Mockito.doThrow(tooOften).when(redisRateLimitChecker).check(rateLimit);

        ApiException exception = assertThrows(ApiException.class, () -> checker.check(rateLimit));

        assertSame(tooOften, exception);
        verify(localRateLimitChecker, never()).check(rateLimit);
    }
}
