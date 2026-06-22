package app.keystone.infrastructure.annotations.ratelimit;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.ServletHolderUtil;
import app.keystone.common.utils.ip.IpUtil;
import app.keystone.infrastructure.user.AuthenticationUtils;
import app.keystone.infrastructure.user.app.AppLoginUser;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 *
 * @author likanug
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key
     */
    String key() default "None";

    /**
     * 限流时间,单位秒
     */
    int time() default 60;

    /**
     * 限流次数
     */
    int maxCount() default 100;

    /**
     * 限流条件类型
     */
    LimitType limitType() default LimitType.GLOBAL;

    /**
     * 限流使用的缓存类型
     */
    CacheType cacheType() default CacheType.REDIS;



    enum LimitType {
        /**
         * 默认策略全局限流  不区分IP和用户
         */
        GLOBAL{
            @Override
            public String generateCombinedKey(RateLimit rateLimiter) {
                return rateLimiter.key() + this.name();
            }
        },

        /**
         * 根据请求者IP进行限流
         */
        IP {
            @Override
            public String generateCombinedKey(RateLimit rateLimiter) {
                String clientIP = IpUtil.getClientIp(ServletHolderUtil.getRequest());
                return rateLimiter.key() + clientIP;
            }
        },

        /**
         * 按Web用户限流
         */
        SYSTEM_USER {
            @Override
            public String generateCombinedKey(RateLimit rateLimiter) {
                SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
                if (loginUser == null) {
                    throw new ApiException(ErrorCode.Client.COMMON_NO_AUTHORIZATION);
                }
                return rateLimiter.key() + loginUser.getUsername();
            }
        },

        /**
         * 按App用户限流
         */
        APP_USER {
            @Override
            public String generateCombinedKey(RateLimit rateLimiter) {
                AppLoginUser loginUser = AuthenticationUtils.getAppLoginUser();
                if (loginUser == null) {
                    throw new ApiException(ErrorCode.Client.COMMON_NO_AUTHORIZATION);
                }
                return rateLimiter.key() + loginUser.getUsername();
            }
        };


        public abstract String generateCombinedKey(RateLimit rateLimiter);

    }

    enum CacheType {

        /**
         * 使用redis做缓存
         */
        REDIS,

        /**
         * 使用map做缓存
         */
        Map

    }

}
