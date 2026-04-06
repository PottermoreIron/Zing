package com.pot.zing.framework.starter.ratelimit.annotation;

import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitPolicyEnum;

import java.lang.annotation.*;

/**
 * Declares a rate limit for a method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Base key. Supports SpEL expressions.
     */
    String key() default "";

    /**
     * Allowed requests per second.
     */
    double rate() default 10.0;

    /**
     * Key strategy type.
     */
    RateLimitMethodEnum type() default RateLimitMethodEnum.FIXED;

    /**
     * Behavior when the rate limit is reached.
     */
    RateLimitPolicyEnum policy() default RateLimitPolicyEnum.REJECT;

    /**
     * Wait timeout in milliseconds when using the wait policy.
     */
    long waitTimeout() default 1000L;

    /**
     * Error message returned when the limit is exceeded.
     */
    String message() default "请求过于频繁，请稍后再试";
}
