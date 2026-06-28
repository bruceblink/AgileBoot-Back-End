package app.keystone.infrastructure.annotations.ratelimit.implementation;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit;
import app.keystone.infrastructure.annotations.ratelimit.RateLimitKeyGenerator;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Local JVM fixed-window rate limit checker.
 */
@Component
@Slf4j
public class LocalRateLimitChecker extends AbstractRateLimitChecker {

    /**
     * At most 4096 active keys are retained. Older keys may be evicted.
     */
    private final Cache<String, FixedWindowCounter> cache = CacheBuilder.newBuilder().maximumSize(4096).build();

    private final Ticker ticker;

    public LocalRateLimitChecker() {
        this(Ticker.systemTicker());
    }

    LocalRateLimitChecker(Ticker ticker) {
        this.ticker = Objects.requireNonNull(ticker, "ticker must not be null");
    }

    @Override
    public void check(RateLimit rateLimit) {
        validate(rateLimit);

        String combinedKey = RateLimitKeyGenerator.generate(rateLimit);
        FixedWindowCounter counter = cache.asMap().computeIfAbsent(combinedKey, key -> new FixedWindowCounter());
        long windowNanos = TimeUnit.SECONDS.toNanos(rateLimit.time());
        if (!counter.tryAcquire(rateLimit.maxCount(), windowNanos, ticker.read())) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_TOO_OFTEN);
        }

        log.debug("限制请求key:{}, combined key:{}", rateLimit.key(), combinedKey);
    }

    private static final class FixedWindowCounter {

        private int count;

        private long expiresAtNanos;

        private synchronized boolean tryAcquire(int maxCount, long windowNanos, long nowNanos) {
            if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
                expiresAtNanos = nowNanos + windowNanos;
                count = 0;
            }
            if (count >= maxCount) {
                return false;
            }
            count++;
            return true;
        }
    }
}
