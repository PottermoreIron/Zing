package com.pot.common.ratelimit.impl;

import com.pot.common.annotations.ratelimit.RateLimit;
import com.pot.common.enums.ratelimit.RateLimitMethodEnum;
import com.pot.common.ratelimit.RateLimitKeyProvider;
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
    public RateLimitMethodEnum getType() {
        return RateLimitMethodEnum.FIXED;
    }
}

