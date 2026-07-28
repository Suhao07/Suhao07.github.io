package com.Auroral.blog.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.Auroral.blog.constant.MQPrefixConst.*;

/**
 * Rabbitmq配置类
 *
 * @author Auroral
 */
@Configuration
public class RabbitConfig {

    @Bean
    public Queue articleQueue() {
        return new Queue(MAXWELL_QUEUE, true);
    }

    @Bean
    public FanoutExchange maxWellExchange() {
        return new FanoutExchange(MAXWELL_EXCHANGE, true, false);
    }

    @Bean
    public Binding bindingArticleDirect() {
        return BindingBuilder.bind(articleQueue()).to(maxWellExchange());
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public FanoutExchange emailExchange() {
        return new FanoutExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public Binding bindingEmailDirect() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange());
    }
    // 添加到RabbitConfig类中
    @Bean
    public Queue smsQueue() {
        return new Queue(SMS_QUEUE, true);
    }

    @Bean
    public FanoutExchange smsExchange() {
        return new FanoutExchange(SMS_EXCHANGE, true, false);
    }

    @Bean
    public Binding bindingSMSDirect() {
        return BindingBuilder.bind(smsQueue()).to(smsExchange());
    }
}
