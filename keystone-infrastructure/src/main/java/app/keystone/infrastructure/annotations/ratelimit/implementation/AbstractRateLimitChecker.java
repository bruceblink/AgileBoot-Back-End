package app.keystone.infrastructure.annotations.ratelimit.implementation;

import app.keystone.infrastructure.annotations.ratelimit.RateLimit;

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

}
