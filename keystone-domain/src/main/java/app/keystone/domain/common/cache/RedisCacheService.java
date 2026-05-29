package app.keystone.domain.common.cache;

import app.keystone.infrastructure.cache.RedisUtil;
import app.keystone.infrastructure.cache.redis.CacheKeyEnum;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.user.web.LoginRefreshSession;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import app.keystone.domain.system.dict.db.SysDictDataEntity;
import app.keystone.domain.system.dict.db.SysDictDataService;
import app.keystone.domain.system.post.db.SysPostEntity;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.post.db.SysPostService;
import app.keystone.domain.system.role.db.SysRoleService;
import app.keystone.domain.system.user.db.SysUserService;
import java.io.Serializable;
import java.util.List;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author valarchie
 */
@Component
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisUtil redisUtil;
    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysPostService postService;
    private final SysDictDataService dictDataService;

    public RedisCacheTemplate<String> captchaCache;
    public RedisCacheTemplate<SystemLoginUser> loginUserCache;
    public RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache;
    public RedisCacheTemplate<String> loginRefreshLockCache;
    public RedisCacheTemplate<String> loginAccountCache;
    public RedisCacheTemplate<SysUserEntity> userCache;
    public RedisCacheTemplate<SysRoleEntity> roleCache;
    public RedisCacheTemplate<SysPostEntity> postCache;
    public RedisCacheTemplate<List<SysDictDataEntity>> dictDataCache;

//    public RedisCacheTemplate<RoleInfo> roleModelInfoCache;

    @PostConstruct
    public void init() {

        captchaCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHAT);

        loginUserCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.LOGIN_USER_KEY);

        loginRefreshTokenCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.LOGIN_REFRESH_TOKEN_KEY);

        loginRefreshLockCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.LOGIN_REFRESH_LOCK_KEY);

        loginAccountCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.LOGIN_ACCOUNT_KEY);

        userCache = new RedisCacheTemplate<SysUserEntity>(redisUtil, CacheKeyEnum.USER_ENTITY_KEY) {
            @Override
            public SysUserEntity getObjectFromDb(Object id) {
                return userService.getById((Serializable) id);
            }
        };

        roleCache = new RedisCacheTemplate<SysRoleEntity>(redisUtil, CacheKeyEnum.ROLE_ENTITY_KEY) {
            @Override
            public SysRoleEntity getObjectFromDb(Object id) {
                return roleService.getById((Serializable) id);
            }
        };

//        roleModelInfoCache = new RedisCacheTemplate<RoleInfo>(redisUtil, CacheKeyEnum.ROLE_MODEL_INFO_KEY) {
//            @Override
//            public RoleInfo getObjectFromDb(Object id) {
//                UserDetailsService userDetailsService = SpringUtil.getBean(UserDetailsService.class);
//                return userDetailsService.getRoleInfo((Long) id);
//            }
//
//        };

        postCache = new RedisCacheTemplate<SysPostEntity>(redisUtil, CacheKeyEnum.POST_ENTITY_KEY) {
            @Override
            public SysPostEntity getObjectFromDb(Object id) {
                return postService.getById((Serializable) id);
            }

        };

        dictDataCache = new RedisCacheTemplate<List<SysDictDataEntity>>(redisUtil, CacheKeyEnum.DICT_DATA_KEY) {
            @Override
            public List<SysDictDataEntity> getObjectFromDb(Object id) {
                return dictDataService.listByDictType(id.toString());
            }
        };


    }


}
