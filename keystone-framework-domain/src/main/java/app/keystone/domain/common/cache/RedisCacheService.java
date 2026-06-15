package app.keystone.domain.common.cache;

import app.keystone.infrastructure.cache.RedisUtil;
import app.keystone.infrastructure.cache.redis.CacheKeyEnum;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.user.web.LoginRefreshSession;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import org.springframework.stereotype.Component;

/**
 * @author likanug
 */
@Component
public class RedisCacheService {

    public RedisCacheTemplate<String> captchaCache;
    public RedisCacheTemplate<SystemLoginUser> loginUserCache;
    public RedisCacheTemplate<LoginRefreshSession> loginRefreshTokenCache;
    public RedisCacheTemplate<String> loginRefreshLockCache;
    public RedisCacheTemplate<String> loginAccountCache;

    public RedisCacheService(RedisUtil redisUtil) {
        captchaCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.CAPTCHA);
        loginUserCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.LOGIN_USER_KEY);
        loginRefreshTokenCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.LOGIN_REFRESH_TOKEN_KEY);
        loginRefreshLockCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.LOGIN_REFRESH_LOCK_KEY);
        loginAccountCache = new RedisCacheTemplate<>(redisUtil, CacheKeyEnum.LOGIN_ACCOUNT_KEY);
    }
}
