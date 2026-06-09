package app.keystone.domain.system.job.runtime;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * Invokes a no-argument Spring bean method configured by a scheduled job.
 * @author likanug
 */
@Component
public class JobInvokeUtil {

    private static final Pattern INVOKE_TARGET = Pattern.compile("^([A-Za-z][A-Za-z0-9_]*?)\\.([A-Za-z][A-Za-z0-9_]*)\\(\\)$");

    private final ApplicationContext applicationContext;

    public JobInvokeUtil(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void validateInvokeTarget(String invokeTarget) {
        ParsedTarget target = parse(invokeTarget);
        Object bean = getBean(target.beanName());
        Method method = ReflectionUtils.findMethod(bean.getClass(), target.methodName());
        if (method == null || method.getParameterCount() != 0) {
            throw new ApiException(ErrorCode.Business.JOB_INVOKE_METHOD_NOT_FOUND, invokeTarget);
        }
    }

    public void invoke(String invokeTarget) {
        ParsedTarget target = parse(invokeTarget);
        Object bean = getBean(target.beanName());
        Method method = ReflectionUtils.findMethod(bean.getClass(), target.methodName());
        if (method == null || method.getParameterCount() != 0) {
            throw new ApiException(ErrorCode.Business.JOB_INVOKE_METHOD_NOT_FOUND, invokeTarget);
        }
        try {
            ReflectionUtils.makeAccessible(method);
            method.invoke(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ApiException(e, ErrorCode.Business.JOB_EXECUTE_FAILED, e.getMessage());
        }
    }

    private ParsedTarget parse(String invokeTarget) {
        Matcher matcher = INVOKE_TARGET.matcher(invokeTarget == null ? "" : invokeTarget.trim());
        if (!matcher.matches()) {
            throw new ApiException(ErrorCode.Business.JOB_INVOKE_TARGET_INVALID);
        }
        return new ParsedTarget(matcher.group(1), matcher.group(2));
    }

    private Object getBean(String beanName) {
        if (!applicationContext.containsBean(beanName)) {
            throw new ApiException(ErrorCode.Business.JOB_INVOKE_BEAN_NOT_FOUND, beanName);
        }
        return applicationContext.getBean(beanName);
    }

    private record ParsedTarget(String beanName, String methodName) {
    }
}
