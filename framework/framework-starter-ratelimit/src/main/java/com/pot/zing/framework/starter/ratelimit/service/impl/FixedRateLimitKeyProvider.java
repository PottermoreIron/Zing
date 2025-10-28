package com.pot.zing.framework.starter.ratelimit.service.impl;

import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitKeyProvider;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author: Pot
 * @created: 2025/10/18 22:05
 * @description: 自定义固定限流Key提供者
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
        return Integer.MAX_VALUE; // 最低优先级
    }
}
