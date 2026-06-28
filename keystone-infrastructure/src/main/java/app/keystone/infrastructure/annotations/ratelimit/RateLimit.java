package app.keystone.infrastructure.annotations.ratelimit;

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

    enum LimitType {
        /**
         * 默认策略全局限流  不区分IP和用户
         */
        GLOBAL,

        /**
         * 根据请求者IP进行限流
         */
        IP,

        /**
         * 按Web用户限流
         */
        SYSTEM_USER,

        /**
         * 按App用户限流
         */
        APP_USER
    }

}
