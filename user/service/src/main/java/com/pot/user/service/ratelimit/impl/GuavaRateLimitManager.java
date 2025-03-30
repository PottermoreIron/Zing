package com.pot.user.service.ratelimit.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import com.pot.user.service.ratelimit.RateLimitManager;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/3/30 16:11
 * @description: 基于Guava RateLimiter的限流管理器实现
 */
@Component
public class GuavaRateLimitManager implements RateLimitManager {
    private final Cache<String, RateLimiter> rateLimiterCache;

    /**
     * 默认构造函数，使用1小时作为默认过期时间
     */
    public GuavaRateLimitManager() {
        this.rateLimiterCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build();
    }

    /**
     * 接收过期时间的构造函数
     *
     * @param expireAfterAccess 访问后过期时间（分钟）
     */
    public GuavaRateLimitManager(int expireAfterAccess) {
        this.rateLimiterCache = CacheBuilder.newBuilder()
                .expireAfterAccess(expireAfterAccess, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit timeUnit) {
        try {
            RateLimiter rateLimiter = rateLimiterCache.get(key, () -> RateLimiter.create(rate));

            if (timeout > 0) {
                return rateLimiter.tryAcquire(timeout, timeUnit);
            } else {
                return rateLimiter.tryAcquire();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
