package com.Auroral.blog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录方式枚举
 *
 * @author: Auroral
 * @date: 2025-03-29
 **/
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {
    /**
     * 邮箱登录
     */
    EMAIL(0, "邮箱登录"),
    /**
     * QQ登录
     */
    QQ(1, "QQ登录"),
    /**
     * 微信登录
     */
    WEICHAT(2,"微信登录"),
    /**
     * 手机号登录
     * */
    PHONE(3,"手机号登录"),
    /**
     * 微博登录
     */
    WEIBO(4, "微博登录"),

    GITHUB(5, "GitHub登录");



    /**
     * 登录方式
     */
    private final Integer type;

    /**
     * 描述
     */
    private final String desc;

}
