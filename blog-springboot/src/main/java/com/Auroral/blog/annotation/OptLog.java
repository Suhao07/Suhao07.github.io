package com.Auroral.blog.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Auroral
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OptLog {

    /**
     * @return 操作类型
     */
    String optType() default "";

}
