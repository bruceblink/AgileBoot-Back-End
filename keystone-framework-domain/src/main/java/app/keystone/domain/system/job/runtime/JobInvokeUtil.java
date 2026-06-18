package app.keystone.domain.system.job.runtime;

import app.keystone.common.annotation.JobTask;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonException;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.job.dto.JobInvokeTargetDTO;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Invokes a Spring bean method configured by a scheduled job.
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
        validateInvokeTarget(invokeTarget, null);
    }

    public void validateInvokeTarget(String invokeTarget, String jobParams) {
        ParsedTarget target = parse(invokeTarget);
        Object bean = getBean(target.beanName());
        Method method = findInvokableMethod(bean, target.methodName());
        if (!isSupportedMethod(method)) {
            throw new ApiException(ErrorCode.Business.JOB_INVOKE_METHOD_NOT_FOUND, invokeTarget);
        }
        validateJobParamsJson(jobParams);
        resolveArguments(method, jobParams);
    }

    public List<JobInvokeTargetDTO> getAvailableInvokeTargets() {
        return Arrays.stream(applicationContext.getBeanDefinitionNames())
            .map(this::getBeanCandidateTargets)
            .flatMap(List::stream)
            .sorted(Comparator.comparing(JobInvokeTargetDTO::getGroup)
                .thenComparing(JobInvokeTargetDTO::getName)
                .thenComparing(JobInvokeTargetDTO::getInvokeTarget))
            .toList();
    }

    public void invoke(String invokeTarget) {
        invoke(invokeTarget, null);
    }

    public void invoke(String invokeTarget, String jobParams) {
        ParsedTarget target = parse(invokeTarget);
        Object bean = getBean(target.beanName());
        Method method = findInvokableMethod(bean, target.methodName());
        if (!isSupportedMethod(method)) {
            throw new ApiException(ErrorCode.Business.JOB_INVOKE_METHOD_NOT_FOUND, invokeTarget);
        }
        try {
            ReflectionUtils.makeAccessible(method);
            method.invoke(bean, resolveArguments(method, jobParams));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ApiException(e, ErrorCode.Business.JOB_EXECUTE_FAILED, e.getMessage());
        }
    }

    private List<JobInvokeTargetDTO> getBeanCandidateTargets(String beanName) {
        Object bean;
        try {
            bean = applicationContext.getBean(beanName);
        } catch (BeansException e) {
            return List.of();
        }

        Class<?> targetClass = AopUtils.getTargetClass(bean);
        return List.of(ReflectionUtils.getUniqueDeclaredMethods(targetClass)).stream()
            .map(method -> toCandidate(beanName, method))
            .filter(Objects::nonNull)
            .toList();
    }

    private JobInvokeTargetDTO toCandidate(String beanName, Method method) {
        if (!isSupportedMethod(method) || Modifier.isStatic(method.getModifiers())) {
            return null;
        }
        JobTask jobTask = AnnotatedElementUtils.findMergedAnnotation(method, JobTask.class);
        boolean scheduled = AnnotatedElementUtils.hasAnnotation(method, Scheduled.class)
            || AnnotatedElementUtils.hasAnnotation(method, Schedules.class);
        if (jobTask == null && !scheduled) {
            return null;
        }

        String methodName = method.getName();
        String name = methodName;
        String group = scheduled ? "scheduled" : "default";
        String description = "";
        if (jobTask != null) {
            name = StringUtils.hasText(jobTask.name()) ? jobTask.name() : methodName;
            group = StringUtils.hasText(jobTask.group()) ? jobTask.group() : group;
            description = jobTask.description();
        }
        String invokeTarget = beanName + "." + methodName + "()";
        return new JobInvokeTargetDTO(invokeTarget, beanName, methodName, name, group, description);
    }

    private Method findInvokableMethod(Object bean, String methodName) {
        Method method = findSupportedMethod(bean.getClass(), methodName);
        if (method != null) {
            return method;
        }
        method = findSupportedMethod(AopUtils.getTargetClass(bean), methodName);
        if (method != null && method.getDeclaringClass().isInstance(bean)) {
            return method;
        }
        return null;
    }

    private Method findSupportedMethod(Class<?> type, String methodName) {
        List<Method> methods = Arrays.stream(ReflectionUtils.getUniqueDeclaredMethods(type))
            .filter(method -> method.getName().equals(methodName))
            .filter(this::isSupportedMethod)
            .toList();
        return methods.size() == 1 ? methods.get(0) : null;
    }

    private boolean isSupportedMethod(Method method) {
        return method != null && method.getParameterCount() <= 1;
    }

    private Object[] resolveArguments(Method method, String jobParams) {
        if (method.getParameterCount() == 0) {
            return new Object[0];
        }
        try {
            Object argument = JacksonUtil.from(
                StringUtils.hasText(jobParams) ? jobParams : "{}",
                method.getGenericParameterTypes()[0]
            );
            return new Object[] {argument};
        } catch (JacksonException e) {
            throw new ApiException(e, ErrorCode.Business.JOB_PARAMS_INVALID, jobParams);
        }
    }

    private void validateJobParamsJson(String jobParams) {
        if (StringUtils.hasText(jobParams) && !JacksonUtil.isJson(jobParams)) {
            throw new ApiException(ErrorCode.Business.JOB_PARAMS_INVALID, jobParams);
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
