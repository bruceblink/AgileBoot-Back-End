package app.keystone.infrastructure.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.infrastructure.cache.RedisUtil;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RedisCacheTemplateTest {

    @Test
    void get_shouldReturnNullAndNotWriteNullWhenRedisMisses() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.getCacheObject("captcha_codes:missing")).thenReturn(null);
        RedisCacheTemplate<String> cache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHA);

        String value = cache.get("missing");

        assertThat(value).isNull();
        verify(redisUtil).getCacheObject("captcha_codes:missing");
        verify(redisUtil, never()).setCacheObject(eq("captcha_codes:missing"), eq(null), eq(2), eq(TimeUnit.MINUTES));
    }

    @Test
    void get_shouldLoadRedisOnceAndReuseLocalCache() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.getCacheObject("captcha_codes:exists")).thenReturn("redis-value");
        RedisCacheTemplate<String> cache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHA);

        String first = cache.get("exists");
        String second = cache.get("exists");

        assertThat(first).isEqualTo("redis-value");
        assertThat(second).isEqualTo("redis-value");
        verify(redisUtil, times(1)).getCacheObject("captcha_codes:exists");
        verify(redisUtil, never()).setCacheObject(eq("captcha_codes:exists"), eq("redis-value"), eq(2),
            eq(TimeUnit.MINUTES));
    }

    @Test
    void set_shouldWriteRedisAndUpdateLocalCache() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        RedisCacheTemplate<String> cache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHA);

        cache.set("code", "1234");

        assertThat(cache.getFromLocal("code")).isEqualTo("1234");
        verify(redisUtil).setCacheObject("captcha_codes:code", "1234", 2, TimeUnit.MINUTES);
        verify(redisUtil, never()).getCacheObject("captcha_codes:code");
    }

    @Test
    void getFromRedis_shouldBypassAndRefreshLocalCache() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.getCacheObject("captcha_codes:code")).thenReturn("fresh");
        RedisCacheTemplate<String> cache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHA);

        String value = cache.getFromRedis("code");

        assertThat(value).isEqualTo("fresh");
        assertThat(cache.getFromLocal("code")).isEqualTo("fresh");
        verify(redisUtil).getCacheObject("captcha_codes:code");
    }

    @Test
    void setIfAbsent_shouldInvalidateLocalCacheWhenRedisRejectsWrite() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.setCacheObjectIfAbsent("captcha_codes:code", "new", 2, TimeUnit.MINUTES)).thenReturn(false);
        RedisCacheTemplate<String> cache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHA);
        cache.set("code", "old");

        boolean result = cache.setIfAbsent("code", "new");

        assertThat(result).isFalse();
        assertThat(cache.getFromLocal("code")).isNull();
        verify(redisUtil).setCacheObjectIfAbsent("captcha_codes:code", "new", 2, TimeUnit.MINUTES);
    }
}
