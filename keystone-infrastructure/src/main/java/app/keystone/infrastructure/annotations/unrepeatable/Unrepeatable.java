package app.keystone.infrastructure.annotations.unrepeatable;

import app.keystone.infrastructure.user.AuthenticationUtils;
import app.keystone.infrastructure.user.app.AppLoginUser;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义注解防止表单重复提交
 * 仅生效于有RequestBody注解的参数  因为使用RequestBodyAdvice来实现
 * @author likanug
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Unrepeatable {

    /**
     * 间隔时间(s)，小于此时间视为重复提交
     */
    int interval() default 5;

    /**
     * 检测条件类型
     */
    CheckType checkType() default CheckType.SYSTEM_USER;

    @Slf4j
    enum CheckType {
        /**
         * 按App用户
         */
        APP_USER {
            @Override
            public String generateResubmitRedisKey(Method method) {
                String username;

                try {
                    AppLoginUser loginUser = AuthenticationUtils.getAppLoginUser();
                    username = loginUser.getUsername();
                } catch (Exception e) {
                    username = NO_LOGIN;
                    log.error("could not find the related user to check repeatable submit.");
                }

                return formatMessage(RESUBMIT_REDIS_KEY,
                    this.name(),
                    method.getDeclaringClass().getName(),
                    method.getName(),
                    username);
            }
        },
        /**
         * 按Web用户
         */
        SYSTEM_USER {
            @Override
            public String generateResubmitRedisKey(Method method) {
                String username;

                try {
                    SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
                    username = loginUser.getUsername();
                } catch (Exception e) {
                    username = NO_LOGIN;
                    log.error("could not find the related user to check repeatable submit.");
                }

                return formatMessage(RESUBMIT_REDIS_KEY,
                    this.name(),
                    method.getDeclaringClass().getName(),
                    method.getName(),
                    username);
            }
        };

        public static final String NO_LOGIN = "Anonymous";
        public static final String RESUBMIT_REDIS_KEY = "resubmit:{}:{}:{}:{}";

        public abstract String generateResubmitRedisKey(Method method);

        private static String formatMessage(String template, Object... args) {
            if (template == null || args == null || args.length == 0) {
                return template;
            }
            String result = template;
            for (Object arg : args) {
                int index = result.indexOf("{}");
                if (index < 0) {
                    break;
                }
                String replacement = String.valueOf(arg);
                result = result.substring(0, index) + replacement + result.substring(index + 2);
            }
            return result;
        }

    }

}
