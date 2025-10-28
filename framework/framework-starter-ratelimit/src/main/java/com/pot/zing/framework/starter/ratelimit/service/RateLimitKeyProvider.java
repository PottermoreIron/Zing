package com.pot.zing.framework.starter.ratelimit.service;

import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author: Pot
 * @created: 2025/10/18 22:02
 * @description: 自定义限流Key提供者接口
 */
public interface RateLimitKeyProvider {

    /**
     * 生成限流key
     *
     * @param baseKey   基础key
     * @param joinPoint 切点
     * @param rateLimit 限流注解
     * @return 最终的限流key
     */
    String generateKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit);

    /**
     * 获取支持的限流类型
     *
     * @return 限流类型
     */
    RateLimitMethodEnum getSupportedType();

    /**
     * 获取优先级，数值越小优先级越高
     *
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }
}
