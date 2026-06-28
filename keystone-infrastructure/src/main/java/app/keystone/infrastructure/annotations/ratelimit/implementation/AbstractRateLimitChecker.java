package app.keystone.infrastructure.annotations.ratelimit.implementation;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit;
import java.util.Objects;

/**
 * @author likanug
 */
public abstract class AbstractRateLimitChecker {

    /**
     * 检查是否超出限流
     *
     * @param rateLimiter RateLimit
     */
    public abstract void check(RateLimit rateLimiter);

    protected void validate(RateLimit rateLimit) {
        Objects.requireNonNull(rateLimit, "rateLimit must not be null");
        if (rateLimit.maxCount() <= 0) {
            throw new ApiException(ErrorCode.Internal.INVALID_PARAMETER, "@RateLimit.maxCount must be greater than 0");
        }
        if (rateLimit.time() <= 0) {
            throw new ApiException(ErrorCode.Internal.INVALID_PARAMETER, "@RateLimit.time must be greater than 0");
        }
    }

}
