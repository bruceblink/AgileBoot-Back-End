package app.keystone.infrastructure.annotations.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Global rate limit configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "keystone.rate-limit")
public class RateLimitProperties {

    /**
     * Rate limit backend. Redis is the production default.
     */
    private RateLimitBackend backend = RateLimitBackend.REDIS;

    /**
     * Whether Redis failures should fall back to the local JVM counter.
     */
    private boolean fallbackToLocal = false;
}
