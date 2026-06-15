package app.keystone.domain.common.cache;

import java.util.function.Function;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class SpringCacheTemplate<T> {

    private final CacheManager cacheManager;
    private final String cacheName;
    private final Function<Object, T> loader;

    public SpringCacheTemplate(CacheManager cacheManager, String cacheName) {
        this(cacheManager, cacheName, ignored -> null);
    }

    public SpringCacheTemplate(CacheManager cacheManager, String cacheName, Function<Object, T> loader) {
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
        this.loader = loader;
    }

    public T get(Object id) {
        if (id == null) {
            return null;
        }
        T cachedValue = getFromCache(id);
        if (cachedValue != null) {
            return cachedValue;
        }
        T value = loader.apply(id);
        if (value != null) {
            set(id, value);
        }
        return value;
    }

    /**
     * @deprecated use {@link #get(Object)}.
     */
    @Deprecated
    public T getObjectById(Object id) {
        return get(id);
    }

    public T getFromCache(Object id) {
        if (id == null) {
            return null;
        }
        Cache cache = cache();
        if (cache == null) {
            return null;
        }
        Cache.ValueWrapper wrapper = cache.get(id);
        if (wrapper == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T value = (T) wrapper.get();
        return value;
    }

    /**
     * @deprecated use {@link #getFromCache(Object)}.
     */
    @Deprecated
    public T getObjectOnlyInCacheById(Object id) {
        return getFromCache(id);
    }

    public void set(Object id, T value) {
        if (id == null || value == null) {
            return;
        }
        Cache cache = cache();
        if (cache != null) {
            cache.put(id, value);
        }
    }

    public void delete(Object id) {
        if (id == null) {
            return;
        }
        Cache cache = cache();
        if (cache != null) {
            cache.evict(id);
        }
    }

    public void deleteAll() {
        invalidateAll();
    }

    public void invalidate(Object id) {
        delete(id);
    }

    public void invalidateAll() {
        Cache cache = cache();
        if (cache != null) {
            cache.clear();
        }
    }

    private Cache cache() {
        return cacheManager == null ? null : cacheManager.getCache(cacheName);
    }
}
