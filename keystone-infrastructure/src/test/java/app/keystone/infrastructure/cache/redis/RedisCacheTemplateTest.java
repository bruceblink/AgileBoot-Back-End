package app.keystone.infrastructure.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.infrastructure.cache.RedisUtil;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RedisCacheTemplateTest {

    @Test
    void getObjectById_shouldReturnNullAndNotWriteNullWhenCacheMissHasNoDbFallback() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.getCacheObject("captcha_codes:missing")).thenReturn(null);
        RedisCacheTemplate<String> cache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHAT);

        String value = cache.getObjectById("missing");

        assertThat(value).isNull();
        verify(redisUtil, never()).setCacheObject(eq("captcha_codes:missing"), eq(null), eq(2), eq(TimeUnit.MINUTES));
    }

    @Test
    void getObjectById_shouldWriteDbFallbackWhenCacheMissLoadsValue() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.getCacheObject("captcha_codes:exists")).thenReturn(null);
        RedisCacheTemplate<String> cache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHAT) {
            @Override
            public String getObjectFromDb(Object id) {
                return "db-" + id;
            }
        };

        String value = cache.getObjectById("exists");

        assertThat(value).isEqualTo("db-exists");
        verify(redisUtil).setCacheObject("captcha_codes:exists", "db-exists", 2, TimeUnit.MINUTES);
    }
}
