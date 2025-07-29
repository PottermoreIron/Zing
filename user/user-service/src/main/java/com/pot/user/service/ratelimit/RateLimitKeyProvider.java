package com.pot.user.service.ratelimit;

import com.pot.user.service.annotations.ratelimit.RateLimit;
import com.pot.user.service.enums.ratelimit.RateLimitMethodEnum;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author: Pot
 * @created: 2025/3/30 20:44
 * @description: 限流key提供者接口，用于支持不同类型的限流
 */
public interface RateLimitKeyProvider {
    /**
     * 获取限流key
     *
     * @param baseKey   基础key
     * @param joinPoint 切点
     * @param rateLimit 限流注解
     * @return 限流key
     */
    String getKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit);

    /**
     * 获取限流类型
     *
     * @return 限流类型
     */
    RateLimitMethodEnum getType();
}