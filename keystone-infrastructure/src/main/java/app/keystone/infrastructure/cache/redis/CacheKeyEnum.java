package app.keystone.infrastructure.cache.redis;

import java.util.concurrent.TimeUnit;

/**
 * @author likanug
 */
public enum CacheKeyEnum {

    /**
     * Redis各类缓存集合
     */
    CAPTCHAT("captcha_codes:", 2, TimeUnit.MINUTES),
    LOGIN_USER_KEY("login_tokens:", 30, TimeUnit.MINUTES),
    LOGIN_REFRESH_TOKEN_KEY("login_refresh_tokens:", 7, TimeUnit.DAYS),
    LOGIN_REFRESH_LOCK_KEY("login_refresh_locks:", 10, TimeUnit.SECONDS),
    LOGIN_ACCOUNT_KEY("login_accounts:", 30, TimeUnit.MINUTES),
    RATE_LIMIT_KEY("rate_limit:", 60, TimeUnit.SECONDS),

    ;


    CacheKeyEnum(String key, int expiration, TimeUnit timeUnit) {
        this.key = key;
        this.expiration = expiration;
        this.timeUnit = timeUnit;
    }

    private final String key;
    private final int expiration;
    private final TimeUnit timeUnit;

    public String key() {
        return key;
    }

    public int expiration() {
        return expiration;
    }

    public TimeUnit timeUnit() {
        return timeUnit;
    }

}
