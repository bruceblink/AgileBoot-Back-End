package app.keystone.infrastructure.annotations.ratelimit.implementation;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author likanug
 */
@SuppressWarnings("UnstableApiUsage")
@Component
@RequiredArgsConstructor
@Slf4j
public class MapRateLimitChecker extends AbstractRateLimitChecker {

    /**
     * 最大仅支持4096个key   超出这个key  限流将可能失效
     */
    private final Cache<String, RateLimiter> cache = CacheBuilder.newBuilder().maximumSize(4096).build();


    @Override
    public void check(RateLimit rateLimit) {
        String combinedKey = rateLimit.limitType().generateCombinedKey(rateLimit);

        RateLimiter rateLimiter = cache.getIfPresent(combinedKey);
        if (rateLimiter == null) {
            rateLimiter = RateLimiter.create((double) rateLimit.maxCount() / rateLimit.time());
            cache.put(combinedKey, rateLimiter);
        }

        if (!rateLimiter.tryAcquire()) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_TOO_OFTEN);
        }

        log.debug("限制请求key:{}, combined key:{}", rateLimit.key(), combinedKey);
    }

}
