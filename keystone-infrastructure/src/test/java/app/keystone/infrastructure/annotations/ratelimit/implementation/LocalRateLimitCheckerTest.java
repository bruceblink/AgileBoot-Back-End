package app.keystone.infrastructure.annotations.ratelimit.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit.LimitType;
import com.google.common.base.Ticker;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class LocalRateLimitCheckerTest {

    private final ManualTicker ticker = new ManualTicker();

    private final LocalRateLimitChecker checker = new LocalRateLimitChecker(ticker);

    @Test
    void check_shouldAllowMaxCountInsideFixedWindow() {
        RateLimit rateLimit = rateLimit(60, 2);

        checker.check(rateLimit);
        checker.check(rateLimit);
        ApiException exception = assertThrows(ApiException.class, () -> checker.check(rateLimit));

        assertEquals(ErrorCode.Client.COMMON_REQUEST_TOO_OFTEN, exception.getErrorCode());
    }

    @Test
    void check_shouldResetCounterAfterWindowExpires() {
        RateLimit rateLimit = rateLimit(1, 1);

        checker.check(rateLimit);
        assertThrows(ApiException.class, () -> checker.check(rateLimit));

        ticker.advance(1, TimeUnit.SECONDS);

        checker.check(rateLimit);
    }

    @Test
    void check_shouldRejectInvalidLimitConfiguration() {
        RateLimit rateLimit = rateLimit(0, 1);

        ApiException exception = assertThrows(ApiException.class, () -> checker.check(rateLimit));

        assertEquals(ErrorCode.Internal.INVALID_PARAMETER, exception.getErrorCode());
    }

    private static RateLimit rateLimit(int time, int maxCount) {
        RateLimit rateLimit = mock(RateLimit.class);
        when(rateLimit.key()).thenReturn("Test:");
        when(rateLimit.limitType()).thenReturn(LimitType.GLOBAL);
        when(rateLimit.time()).thenReturn(time);
        when(rateLimit.maxCount()).thenReturn(maxCount);
        return rateLimit;
    }

    private static final class ManualTicker extends Ticker {

        private long nanos;

        @Override
        public long read() {
            return nanos;
        }

        void advance(long time, TimeUnit timeUnit) {
            nanos += timeUnit.toNanos(time);
        }
    }
}
