package app.keystone.infrastructure.annotations.ratelimit;

/**
 * Backend used by the rate limit checker.
 */
public enum RateLimitBackend {

    /**
     * Use Redis as the shared rate limit store.
     */
    REDIS,

    /**
     * Use local JVM memory as the rate limit store.
     */
    LOCAL
}
