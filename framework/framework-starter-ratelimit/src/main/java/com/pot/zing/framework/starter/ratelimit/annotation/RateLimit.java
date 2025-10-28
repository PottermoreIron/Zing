package com.pot.zing.framework.starter.ratelimit.annotation;

import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitPolicyEnum;

import java.lang.annotation.*;

/**
 * @author: Pot
 * @created: 2025/10/18 22:10
 * @description: 自定义限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 限流速率（每秒允许的请求数）
     */
    double rate() default 10.0;

    /**
     * 限流类型
     */
    RateLimitMethodEnum type() default RateLimitMethodEnum.FIXED;

    /**
     * 限流策略
     */
    RateLimitPolicyEnum policy() default RateLimitPolicyEnum.REJECT;

    /**
     * 等待超时时间（毫秒），仅在策略为WAIT时有效
     */
    long waitTimeout() default 1000L;

    /**
     * 自定义错误消息
     */
    String message() default "请求过于频繁，请稍后再试";
}
