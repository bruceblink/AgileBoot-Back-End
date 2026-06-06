package app.keystone.domain.common.cache;

import app.keystone.domain.system.dict.db.SysDictDataEntity;
import app.keystone.domain.system.dict.db.SysDictDataService;
import app.keystone.domain.system.post.db.SysPostEntity;
import app.keystone.domain.system.post.db.SysPostService;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.role.db.SysRoleService;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.infrastructure.cache.aop.CacheNameConstants;
import java.io.Serializable;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class SpringCacheService {

    public final SpringCacheTemplate<SysUserEntity> userCache;
    public final SpringCacheTemplate<SysRoleEntity> roleCache;
    public final SpringCacheTemplate<SysPostEntity> postCache;
    public final SpringCacheTemplate<List<SysDictDataEntity>> dictDataCache;

    public SpringCacheService(CacheManager cacheManager, SysUserService userService, SysRoleService roleService,
        SysPostService postService, SysDictDataService dictDataService) {
        userCache = new SpringCacheTemplate<>(cacheManager, CacheNameConstants.USER_ENTITY,
            id -> userService.getById((Serializable) id));
        roleCache = new SpringCacheTemplate<>(cacheManager, CacheNameConstants.ROLE_ENTITY,
            id -> roleService.getById((Serializable) id));
        postCache = new SpringCacheTemplate<>(cacheManager, CacheNameConstants.POST_ENTITY,
            id -> postService.getById((Serializable) id));
        dictDataCache = new SpringCacheTemplate<>(cacheManager, CacheNameConstants.DICT_DATA,
            id -> dictDataService.listByDictType(id.toString()));
    }
}
