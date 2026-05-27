package app.keystone.domain.common.cache;

import app.keystone.infrastructure.cache.caffeine.AbstractCaffeineCacheTemplate;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import app.keystone.domain.system.dept.db.SysDeptEntity;
import app.keystone.domain.system.dict.db.SysDictDataEntity;
import app.keystone.domain.system.post.db.SysPostEntity;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.user.db.SysUserEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 缓存中心  提供全局访问点
 * 如果是领域类的缓存  可以自己新建一个直接放在CacheCenter   不用放在infrastructure包里的LocalCacheService
 * 或者RedisCacheService
 * @author valarchie
 */
@Component
public class CacheCenter {

    private static LocalCacheService localCacheService;
    private static RedisCacheService redisCacheService;

    public CacheCenter(LocalCacheService localCacheService, RedisCacheService redisCacheService) {
        CacheCenter.localCacheService = localCacheService;
        CacheCenter.redisCacheService = redisCacheService;
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

    public static RedisCacheTemplate<String> loginAccountCache() {
        return redisCacheService.loginAccountCache;
    }

    public static RedisCacheTemplate<SysUserEntity> userCache() {
        return redisCacheService.userCache;
    }

    public static RedisCacheTemplate<SysRoleEntity> roleCache() {
        return redisCacheService.roleCache;
    }

    public static RedisCacheTemplate<SysPostEntity> postCache() {
        return redisCacheService.postCache;
    }

    public static RedisCacheTemplate<List<SysDictDataEntity>> dictDataCache() {
        return redisCacheService.dictDataCache;
    }

}

