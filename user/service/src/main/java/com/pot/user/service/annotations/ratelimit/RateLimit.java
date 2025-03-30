package com.pot.user.service.annotations.ratelimit;

import com.pot.user.service.enums.ratelimit.LimitPolicy;
import com.pot.user.service.enums.ratelimit.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: Pot
 * @created: 2025/3/30 16:07
 * @description: 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 限流的key，用于区分不同的限流场景
     * 默认使用类名+方法名作为key
     */
    String key() default "";

    /**
     * 限流速率（每秒请求数）
     */
    double rate();

    /**
     * 限流策略，默认为固定窗口
     */
    RateLimitType type() default RateLimitType.FIXED;

    /**
     * 当限流触发时的处理策略
     */
    LimitPolicy policy() default LimitPolicy.REJECT;

    /**
     * 等待获取令牌的最大时间（毫秒），仅在LimitPolicy.WAIT策略下有效
     */
    long waitTimeout() default 0;

}
