package app.keystone.infrastructure.cache.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地缓存抽象模板（基于 Caffeine 实现的二级缓存）
 * <p>
 * 使用 Caffeine 替代 Guava Cache，Caffeine 在吞吐量和命中率方面均优于 Guava Cache，
 * 且不依赖 {@code sun.misc.Unsafe}，对现代 JDK 更加友好。
 * </p>
 *
 * @param <T> 缓存值类型
 * @author likanug
 */
@Slf4j
public abstract class AbstractCaffeineCacheTemplate<T> {

    private final LoadingCache<String, Optional<T>> caffeineCache = Caffeine.newBuilder()
        // 基于容量回收：超出后按 LRU 淘汰
        .maximumSize(1024)
        // 写入后 5 分钟刷新（异步刷新，刷新期间返回旧值）
        .refreshAfterWrite(5L, TimeUnit.MINUTES)
        // 移除监听
        .removalListener((key, value, cause) ->
            log.info("触发删除动作，删除的key={}, value={}", key, value))
        // 开启缓存统计
        .recordStats()
        // 初始容量
        .initialCapacity(128)
        .build(key -> {
            T cacheObject = getObjectFromDb(key);
            log.debug("find the local caffeine cache of key: {} is {}", key, cacheObject);
            return Optional.ofNullable(cacheObject);
        });

    /**
     * 从缓存中获取对象，缓存未命中时回调 {@link #getObjectFromDb} 加载
     *
     * @param key 缓存键
     * @return 缓存对象，不存在时返回 {@code null}
     */
    public T get(Object key) {
        if (Objects.isNull(key)) {
            return null;
        }
        Optional<T> optional = caffeineCache.get(String.valueOf(key));
        return optional != null ? optional.orElse(null) : null;
    }

    /**
     * 使指定 key 的缓存失效
     *
     * @param key 缓存键
     */
    public void invalidate(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        caffeineCache.invalidate(key);
    }

    /**
     * 清空所有缓存
     */
    public void invalidateAll() {
        caffeineCache.invalidateAll();
    }

    /**
     * 子类实现：当缓存未命中时从数据源加载数据
     *
     * @param id 数据标识
     * @return 数据对象，不存在时返回 {@code null}
     */
    public abstract T getObjectFromDb(Object id);
}
