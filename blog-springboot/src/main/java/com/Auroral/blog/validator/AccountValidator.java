package com.Auroral.blog.validator;

import com.Auroral.blog.annotation.ValidAccount;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class AccountValidator implements ConstraintValidator<ValidAccount, String> {

    // 邮箱验证正则
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9\\u4e00-\\u9fa5]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");

    // 手机号验证正则
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // 1. 先检查是否是手机号
        if (PHONE_PATTERN.matcher(value).matches()) {
            return true;
        }

        // 2. 再检查是否是邮箱
        return EMAIL_PATTERN.matcher(value).matches();
    }
}