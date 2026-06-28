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
 * @author likanug
 */
@Component
@Slf4j
public class MapRateLimitChecker extends AbstractRateLimitChecker {

    /**
     * 最大仅支持4096个key   超出这个key  限流将可能失效
     */
    private final Cache<String, FixedWindowCounter> cache = CacheBuilder.newBuilder().maximumSize(4096).build();

    private final Ticker ticker;

    public MapRateLimitChecker() {
        this(Ticker.systemTicker());
    }

    MapRateLimitChecker(Ticker ticker) {
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
