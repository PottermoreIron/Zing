package com.pot.zing.framework.starter.ratelimit.service;

import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Strategy for building rate-limit keys.
 */
public interface RateLimitKeyProvider {

    /**
     * Builds the final rate-limit key.
     */
    String generateKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit);

    /**
     * Returns the supported key strategy type.
     */
    RateLimitMethodEnum getSupportedType();

    /**
     * Returns the strategy order. Lower values run first.
     */
    default int getOrder() {
        return 0;
    }
}
