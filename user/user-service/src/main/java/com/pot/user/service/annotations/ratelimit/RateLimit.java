package com.pot.user.service.annotations.ratelimit;

import com.pot.user.service.enums.ratelimit.RateLimitMethodEnum;
import com.pot.user.service.enums.ratelimit.RateLimitPolicyEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

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
     * 请求数量，表示在指定的时间窗口内允许的最大请求次数
     */
    int count();

    /**
     * 限流时间窗口，单位为timeUnit
     * 默认1秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 限流策略，默认为固定窗口
     */
    RateLimitMethodEnum type() default RateLimitMethodEnum.FIXED;

    /**
     * 当限流触发时的处理策略
     */
    RateLimitPolicyEnum policy() default RateLimitPolicyEnum.REJECT;

    /**
     * 等待获取令牌的最大时间（毫秒），仅在LimitPolicy.WAIT策略下有效
     */
    long waitTimeout() default 0;

}
