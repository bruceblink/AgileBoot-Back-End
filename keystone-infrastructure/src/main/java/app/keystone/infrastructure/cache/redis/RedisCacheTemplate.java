package app.keystone.infrastructure.cache.redis;

import app.keystone.infrastructure.cache.RedisUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 缓存模板，使用本地 Caffeine 缓存 Redis 读取结果。
 *
 * <p>默认读取流程为本地缓存 -> Redis，不包含 DB fallback。需要强一致读取时使用
 * {@link #getFromRedis(Object)} 绕过本地缓存。</p>
 *
 * @author likanug
 */
@Slf4j
public class RedisCacheTemplate<T> {

    private final RedisUtil redisUtil;
    private final CacheKeyEnum cacheKey;
    private final LoadingCache<String, Optional<T>> caffeineCache;

    public RedisCacheTemplate(RedisUtil redisUtil, CacheKeyEnum cacheKey) {
        this.redisUtil = redisUtil;
        this.cacheKey = cacheKey;
        // Caffeine 不支持 softValues；用 maximumSize 限制容量，配合 expireAfterWrite 控制生命周期
        this.caffeineCache = Caffeine.newBuilder()
            // 基于容量回收：超出后按 LRU 淘汰
            .maximumSize(1024)
            // 写入后到期失效，时间单位与 Redis key 配置保持一致
            .expireAfterWrite(cacheKey.expiration(), cacheKey.timeUnit())
            // 初始容量
            .initialCapacity(128)
            .build(cachedKey -> {
                T cacheObject = redisUtil.getCacheObject(cachedKey);
                log.debug("find the redis cache of key: {} is {}", cachedKey, cacheObject);
                return Optional.ofNullable(cacheObject);
            });
    }

    /**
     * 从本地缓存获取对象；本地未命中时从 Redis 加载。
     *
     * @param id id
     */
    public T get(Object id) {
        String cachedKey = generateKey(id);
        if (cachedKey == null) {
            return null;
        }
        return caffeineCache.get(cachedKey).orElse(null);
    }

    /**
     * @deprecated use {@link #get(Object)}.
     */
    @Deprecated
    public T getObjectById(Object id) {
        return get(id);
    }

    /**
     * 只读取本地 Caffeine 缓存，不触发 Redis 加载。
     *
     * @param id id
     */
    public T getFromLocal(Object id) {
        String cachedKey = generateKey(id);
        return getLocalByFullKey(cachedKey);
    }

    /**
     * @deprecated use {@link #get(Object)}. The old name is misleading because it may load Redis.
     */
    @Deprecated
    public T getObjectOnlyInCacheById(Object id) {
        return get(id);
    }

    /**
     * 只读取本地 Caffeine 缓存，不触发 Redis 加载。
     */
    public T getFromLocalByFullKey(String cachedKey) {
        return getLocalByFullKey(cachedKey);
    }

    private T getLocalByFullKey(String cachedKey) {
        if (cachedKey == null || cachedKey.isEmpty()) {
            return null;
        }
        log.debug("find the caffeine cache of key: {}", cachedKey);
        Optional<T> optional = caffeineCache.getIfPresent(cachedKey);
        return optional == null ? null : optional.orElse(null);
    }

    /**
     * 直接从 Redis 读取，绕过本地 Caffeine 缓存。
     */
    public T getFromRedis(Object id) {
        String cachedKey = generateKey(id);
        if (cachedKey == null) {
            return null;
        }
        T cacheObject = redisUtil.getCacheObject(cachedKey);
        if (cacheObject == null) {
            caffeineCache.invalidate(cachedKey);
        } else {
            caffeineCache.put(cachedKey, Optional.of(cacheObject));
        }
        return cacheObject;
    }

    /**
     * @deprecated use {@link #getFromRedis(Object)}.
     */
    @Deprecated
    public T getObjectOnlyInRedisById(Object id) {
        return getFromRedis(id);
    }

    /**
     * 通过完整 Redis key 读取，本地未命中时从 Redis 加载。
     *
     * @param cachedKey 完整 Redis key
     */
    public T getByFullKey(String cachedKey) {
        if (cachedKey == null || cachedKey.isEmpty()) {
            return null;
        }
        log.debug("find the caffeine cache of key: {}", cachedKey);
        return caffeineCache.get(cachedKey).orElse(null);
    }

    /**
     * @deprecated use {@link #getByFullKey(String)}.
     */
    @Deprecated
    public T getObjectOnlyInCacheByKey(String cachedKey) {
        return getByFullKey(cachedKey);
    }

    public void set(Object id, T obj) {
        set(id, obj, cacheKey.expiration(), cacheKey.timeUnit());
    }

    public void set(Object id, T obj, Integer timeout, TimeUnit timeUnit) {
        String cachedKey = generateKey(id);
        if (cachedKey == null || obj == null) {
            return;
        }
        redisUtil.setCacheObject(cachedKey, obj, timeout, timeUnit);
        caffeineCache.put(cachedKey, Optional.of(obj));
    }

    public boolean setIfAbsent(Object id, T obj) {
        return setIfAbsent(id, obj, cacheKey.expiration(), cacheKey.timeUnit());
    }

    public boolean setIfAbsent(Object id, T obj, Integer timeout, TimeUnit timeUnit) {
        String cachedKey = generateKey(id);
        if (cachedKey == null || obj == null) {
            return false;
        }
        Boolean result = redisUtil.setCacheObjectIfAbsent(cachedKey, obj, timeout, timeUnit);
        if (Boolean.TRUE.equals(result)) {
            caffeineCache.put(cachedKey, Optional.of(obj));
            return true;
        }
        caffeineCache.invalidate(cachedKey);
        return false;
    }

    public void delete(Object id) {
        String cachedKey = generateKey(id);
        if (cachedKey == null) {
            return;
        }
        redisUtil.deleteObject(cachedKey);
        caffeineCache.invalidate(cachedKey);
    }

    public void refresh(Object id) {
        String cachedKey = generateKey(id);
        if (cachedKey == null) {
            return;
        }
        redisUtil.expire(cachedKey, cacheKey.expiration(), cacheKey.timeUnit());
        caffeineCache.invalidate(cachedKey);
    }

    public String generateKey(Object id) {
        return id == null ? null : cacheKey.prefix() + id;
    }
}
