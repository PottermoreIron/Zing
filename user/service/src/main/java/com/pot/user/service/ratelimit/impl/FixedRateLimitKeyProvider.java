package com.pot.user.service.ratelimit.impl;

import com.pot.user.service.annotations.ratelimit.RateLimit;
import com.pot.user.service.enums.ratelimit.RateLimitType;
import com.pot.user.service.ratelimit.RateLimitKeyProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/3/30 20:45
 * @description: 固定限流key提供者
 */
@Component
public class FixedRateLimitKeyProvider implements RateLimitKeyProvider {
    @Override
    public String getKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        return baseKey;
    }

    @Override
    public RateLimitType getType() {
        return RateLimitType.FIXED;
    }
}

