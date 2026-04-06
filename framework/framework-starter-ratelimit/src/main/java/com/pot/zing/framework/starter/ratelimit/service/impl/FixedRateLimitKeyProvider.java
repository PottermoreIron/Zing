package com.pot.zing.framework.starter.ratelimit.service.impl;

import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitKeyProvider;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Key provider that returns the base key unchanged.
 */
public class FixedRateLimitKeyProvider implements RateLimitKeyProvider {

    @Override
    public String generateKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        return baseKey;
    }

    @Override
    public RateLimitMethodEnum getSupportedType() {
        return RateLimitMethodEnum.FIXED;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE; // Lowest priority fallback.
    }
}
