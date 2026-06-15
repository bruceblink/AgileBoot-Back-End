package app.keystone.infrastructure.cache.redis;

import app.keystone.infrastructure.cache.RedisUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存接口实现类 三级缓存
 * @author likanug
 */
@Slf4j
public class RedisCacheTemplate<T> {

    private final RedisUtil redisUtil;
    private final CacheKeyEnum redisRedisEnum;
    private final LoadingCache<String, Optional<T>> caffeineCache;

    public RedisCacheTemplate(RedisUtil redisUtil, CacheKeyEnum redisRedisEnum) {
        this.redisUtil = redisUtil;
        this.redisRedisEnum = redisRedisEnum;
        // Caffeine 不支持 softValues；用 maximumSize 限制容量，配合 expireAfterWrite 控制生命周期
        this.caffeineCache = Caffeine.newBuilder()
            // 基于容量回收：超出后按 LRU 淘汰
            .maximumSize(1024)
            // 写入后到期失效
            .expireAfterWrite(redisRedisEnum.expiration(), TimeUnit.MINUTES)
            // 初始容量
            .initialCapacity(128)
            .build(cachedKey -> {
                T cacheObject = getRedisUtil().getCacheObject(cachedKey);
                log.debug("find the redis cache of key: {} is {}", cachedKey, cacheObject);
                return Optional.ofNullable(cacheObject);
            });
    }

    private RedisUtil getRedisUtil() {
        return redisUtil;
    }

    /**
     * 从缓存中获取对象   如果获取不到的话  从DB层面获取
     *
     * @param id id
     */
    public T getObjectById(Object id) {
        String cachedKey = generateKey(id);
        Optional<T> optional = caffeineCache.get(cachedKey);

        if (optional.isEmpty()) {
            T objectFromDb = getObjectFromDb(id);
            if (objectFromDb != null) {
                set(id, objectFromDb);
            }
            return objectFromDb;
        }

        return optional.get();
    }

    /**
     * 从缓存中获取 对象， 即使找不到的话 也不从DB中找
     * @param id id
     */
    public T getObjectOnlyInCacheById(Object id) {
        String cachedKey = generateKey(id);
        log.debug("find the caffeine cache of key: {}", cachedKey);
        Optional<T> optional = caffeineCache.get(cachedKey);
        return optional.isPresent() ? optional.orElse(null) : null;
    }

    /**
     * 直接从 Redis 读取，绕过本地 Caffeine 缓存。
     */
    public T getObjectOnlyInRedisById(Object id) {
        String cachedKey = generateKey(id);
        T cacheObject = getRedisUtil().getCacheObject(cachedKey);
        if (cacheObject == null) {
            caffeineCache.invalidate(cachedKey);
        }
        return cacheObject;
    }

    /**
     * 从缓存中获取 对象， 即使找不到的话 也不从DB中找
     * @param cachedKey 直接通过redis的key来搜索
     */
    public T getObjectOnlyInCacheByKey(String cachedKey) {
        log.debug("find the caffeine cache of key: {}", cachedKey);
        Optional<T> optional = caffeineCache.get(cachedKey);
        return optional.isPresent() ? optional.orElse(null) : null;
    }


    public void set(Object id, T obj) {
        getRedisUtil().setCacheObject(generateKey(id), obj, redisRedisEnum.expiration(), redisRedisEnum.timeUnit());
        caffeineCache.invalidate(generateKey(id));
    }

    public void set(Object id, T obj, Integer timeout, TimeUnit timeUnit) {
        getRedisUtil().setCacheObject(generateKey(id), obj, timeout, timeUnit);
        caffeineCache.invalidate(generateKey(id));
    }

    public boolean setIfAbsent(Object id, T obj) {
        Boolean result = getRedisUtil().setCacheObjectIfAbsent(generateKey(id), obj, redisRedisEnum.expiration(),
            redisRedisEnum.timeUnit());
        caffeineCache.invalidate(generateKey(id));
        return Boolean.TRUE.equals(result);
    }

    public boolean setIfAbsent(Object id, T obj, Integer timeout, TimeUnit timeUnit) {
        Boolean result = getRedisUtil().setCacheObjectIfAbsent(generateKey(id), obj, timeout, timeUnit);
        caffeineCache.invalidate(generateKey(id));
        return Boolean.TRUE.equals(result);
    }

    public void delete(Object id) {
        getRedisUtil().deleteObject(generateKey(id));
        caffeineCache.invalidate(generateKey(id));
    }

    public void refresh(Object id) {
        getRedisUtil().expire(generateKey(id), redisRedisEnum.expiration(), redisRedisEnum.timeUnit());
        caffeineCache.invalidate(generateKey(id));
    }

    public String generateKey(Object id) {
        return redisRedisEnum.key() + id;
    }

    public T getObjectFromDb(Object id) {
        return null;
    }

}
