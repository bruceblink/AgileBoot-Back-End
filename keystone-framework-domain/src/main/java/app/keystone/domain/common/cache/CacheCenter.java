package app.keystone.domain.common.cache;

import app.keystone.common.enums.dictionary.DictionaryData;
import app.keystone.domain.system.dept.db.SysDeptEntity;
import app.keystone.domain.system.dict.db.SysDictDataEntity;
import app.keystone.domain.system.post.db.SysPostEntity;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.infrastructure.cache.caffeine.AbstractCaffeineCacheTemplate;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.user.web.LoginRefreshSession;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 缓存中心  提供全局访问点
 * 如果是领域类的缓存  可以自己新建一个直接放在CacheCenter   不用放在infrastructure包里的LocalCacheService
 * 或者RedisCacheService
 * @author likanug
 */
@Component
public class CacheCenter {

    private static LocalCacheService localCacheService;
    private static RedisCacheService redisCacheService;
    private static SpringCacheService springCacheService;

    public CacheCenter(LocalCacheService localCacheService, RedisCacheService redisCacheService,
        SpringCacheService springCacheService) {
        CacheCenter.localCacheService = localCacheService;
        CacheCenter.redisCacheService = redisCacheService;
        CacheCenter.springCacheService = springCacheService;
    }

    public static AbstractCaffeineCacheTemplate<String> configCache() {
        return localCacheService.configCache;
    }

    public static AbstractCaffeineCacheTemplate<SysDeptEntity> deptCache() {
        return localCacheService.deptCache;
    }

    public static RedisCacheTemplate<String> captchaCache() {
        return redisCacheService.captchaCache;
    }

    public static RedisCacheTemplate<SystemLoginUser> loginUserCache() {
        return redisCacheService.loginUserCache;
    }

    public static RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache() {
        return redisCacheService.loginRefreshTokenCache;
    }

    public static RedisCacheTemplate<String> loginRefreshLockCache() {
        return redisCacheService.loginRefreshLockCache;
    }

    public static RedisCacheTemplate<String> loginAccountCache() {
        return redisCacheService.loginAccountCache;
    }

    public static SpringCacheTemplate<SysUserEntity> userCache() {
        return springCacheService == null ? null : springCacheService.userCache;
    }

    public static SpringCacheTemplate<SysRoleEntity> roleCache() {
        return springCacheService == null ? null : springCacheService.roleCache;
    }

    public static SpringCacheTemplate<SysPostEntity> postCache() {
        return springCacheService == null ? null : springCacheService.postCache;
    }

    public static SpringCacheTemplate<List<SysDictDataEntity>> dictDataCache() {
        return springCacheService == null ? null : springCacheService.dictDataCache;
    }

    public static SpringCacheTemplate<Map<String, List<DictionaryData>>> dictionaryDataMapCache() {
        return springCacheService == null ? null : springCacheService.dictionaryDataMapCache;
    }

    public static <T> SpringCacheTemplate<T> springCache(String cacheName) {
        return springCacheService == null ? null : springCacheService.cache(cacheName);
    }

}

