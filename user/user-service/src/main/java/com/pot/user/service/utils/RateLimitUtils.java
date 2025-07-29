package com.pot.user.service.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/3/30 20:09
 * @description: 限流工具类
 */
public class RateLimitUtils {
    private static final Cache<String, RateLimiter> RATE_LIMITER_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    /**
     * 尝试获取令牌
     *
     * @param key  限流key
     * @param rate 限流速率（每秒请求数）
     * @return 是否获取成功
     */
    public static boolean tryAcquire(String key, double rate) {
        try {
            RateLimiter rateLimiter = RATE_LIMITER_CACHE.get(key, () -> RateLimiter.create(rate));
            return rateLimiter.tryAcquire();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 尝试在指定时间内获取令牌
     *
     * @param key      限流key
     * @param rate     限流速率（每秒请求数）
     * @param timeout  等待超时时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    public static boolean tryAcquire(String key, double rate, long timeout, TimeUnit timeUnit) {
        try {
            RateLimiter rateLimiter = RATE_LIMITER_CACHE.get(key, () -> RateLimiter.create(rate));
            return rateLimiter.tryAcquire(timeout, timeUnit);
        } catch (Exception e) {
            return false;
        }
    }
}
