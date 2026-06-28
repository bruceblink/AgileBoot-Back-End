package app.keystone.infrastructure.annotations.ratelimit;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.annotations.ratelimit.implementation.LocalRateLimitChecker;
import app.keystone.infrastructure.annotations.ratelimit.implementation.RedisRateLimitChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Single entry point for rate limit checks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitChecker {

    private final RedisRateLimitChecker redisRateLimitChecker;

    private final LocalRateLimitChecker localRateLimitChecker;

    private final RateLimitProperties properties;

    public void check(RateLimit rateLimit) {
        if (properties.getBackend() == RateLimitBackend.LOCAL) {
            localRateLimitChecker.check(rateLimit);
            return;
        }

        try {
            redisRateLimitChecker.check(rateLimit);
        } catch (ApiException e) {
            if (!isCacheFailure(e) || !properties.isFallbackToLocal()) {
                throw e;
            }
            log.warn("Redis rate limit check failed, falling back to local cache. key: {}", rateLimit.key(), e);
            localRateLimitChecker.check(rateLimit);
        }
    }

    private boolean isCacheFailure(ApiException e) {
        return e.getErrorCode() == ErrorCode.Internal.GET_CACHE_FAILED;
    }
}
