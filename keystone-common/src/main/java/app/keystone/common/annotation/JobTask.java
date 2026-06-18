package app.keystone.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a no-argument Spring bean method as available for database-backed scheduled jobs.
 *
 * @author likanug
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JobTask {

    /**
     * Display name shown in job target candidates.
     */
    String name() default "";

    /**
     * Logical group shown in job target candidates.
     */
    String group() default "default";

    /**
     * Optional description for operators.
     */
    String description() default "";
}
