package app.keystone.infrastructure.annotations.ratelimit.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit.LimitType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisRateLimitCheckerTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);

    private final RedisRateLimitChecker checker = new RedisRateLimitChecker(redisTemplate);

    @Test
    @SuppressWarnings("unchecked")
    void check_shouldUseGeneratedKeyAndAllowWithinLimit() {
        RateLimit rateLimit = rateLimit(60, 2);
        whenExecute().thenReturn(2L);

        checker.check(rateLimit);

        ArgumentCaptor<List<Object>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(ArgumentMatchers.<RedisScript<Long>>any(), keysCaptor.capture(),
            argsCaptor.capture());
        assertEquals(List.of("Test:GLOBAL:GLOBAL"), keysCaptor.getValue());
        assertEquals(2, argsCaptor.getValue()[0]);
        assertEquals(60, argsCaptor.getValue()[1]);
    }

    @Test
    void check_shouldRejectWhenRedisCountExceedsLimit() {
        RateLimit rateLimit = rateLimit(60, 2);
        whenExecute().thenReturn(3L);

        ApiException exception = assertThrows(ApiException.class, () -> checker.check(rateLimit));

        assertEquals(ErrorCode.Client.COMMON_REQUEST_TOO_OFTEN, exception.getErrorCode());
    }

    @Test
    void check_shouldTranslateRedisFailureToCacheFailure() {
        RateLimit rateLimit = rateLimit(60, 2);
        whenExecute().thenThrow(new IllegalStateException("redis unavailable"));

        ApiException exception = assertThrows(ApiException.class, () -> checker.check(rateLimit));

        assertEquals(ErrorCode.Internal.GET_CACHE_FAILED, exception.getErrorCode());
    }

    @Test
    void check_shouldRejectInvalidLimitConfigurationBeforeRedisAccess() {
        RateLimit rateLimit = rateLimit(0, 2);

        ApiException exception = assertThrows(ApiException.class, () -> checker.check(rateLimit));

        assertEquals(ErrorCode.Internal.INVALID_PARAMETER, exception.getErrorCode());
    }

    @SuppressWarnings("unchecked")
    private org.mockito.stubbing.OngoingStubbing<Long> whenExecute() {
        return when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(),
            ArgumentMatchers.<List<Object>>any(), any(Object[].class)));
    }

    private static RateLimit rateLimit(int time, int maxCount) {
        RateLimit rateLimit = mock(RateLimit.class);
        when(rateLimit.key()).thenReturn("Test:");
        when(rateLimit.limitType()).thenReturn(LimitType.GLOBAL);
        when(rateLimit.time()).thenReturn(time);
        when(rateLimit.maxCount()).thenReturn(maxCount);
        return rateLimit;
    }
}
