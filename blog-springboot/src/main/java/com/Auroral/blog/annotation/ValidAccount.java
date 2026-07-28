package com.Auroral.blog.annotation;


/**
 * author: Auroral
 * date: 2025/3/29
 * description: 自定义注解，用于验证用户是否存在
 */
import com.Auroral.blog.validator.AccountValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AccountValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAccount {
    String message() default "账号格式不正确";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
