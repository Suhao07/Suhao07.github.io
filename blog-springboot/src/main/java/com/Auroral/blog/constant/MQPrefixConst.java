package com.Auroral.blog.constant;

/**
 * mq常量
 *
 * @author: Auroral
 * @date: 2021-04-16
 **/
public class MQPrefixConst {

    /**
     * maxwell交换机
     */
    public static final String MAXWELL_EXCHANGE = "maxwell";

    /**
     * maxwell队列
     */
    public static final String MAXWELL_QUEUE = "article";

    /**
     * email交换机
     */
    public static final String EMAIL_EXCHANGE = "send";

    /**
     * 邮件队列
     */
    public static final String EMAIL_QUEUE = "email";
    /**
     * 短信交换机
     */
    public static final String SMS_EXCHANGE = "sms_exchange";

    /**
     * 短信队列
     */
    public static final String SMS_QUEUE = "sms_queue";
}
