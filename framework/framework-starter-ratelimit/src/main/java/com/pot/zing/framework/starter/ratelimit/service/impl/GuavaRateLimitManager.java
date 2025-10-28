package com.pot.zing.framework.starter.ratelimit.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import com.pot.zing.framework.starter.ratelimit.properties.RateLimitProperties;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitManager;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/10/18 22:05
 * @description: 自定义Guava限流管理器实现
 */
@Slf4j
public class GuavaRateLimitManager implements RateLimitManager {

    private static final String TYPE = "guava";

    private final Cache<String, RateLimiter> rateLimiterCache;
    private final RateLimitProperties properties;

    public GuavaRateLimitManager(RateLimitProperties properties) {
        this.properties = properties;
        this.rateLimiterCache = CacheBuilder.newBuilder()
                .expireAfterAccess(properties.getExpireAfterAccess(), TimeUnit.HOURS)
                .maximumSize(10000)
                .build();
    }

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit timeUnit) {
        log.debug("Guava限流尝试获取令牌 - key: {}, rate: {}, timeout: {}", key, rate, timeout);

        // 应用全局速率因子
        rate = rate * properties.getGlobalRateFactor();

        // 检查速率覆盖配置
        Double overrideRate = properties.getRateOverrides().get(key);
        if (overrideRate != null) {
            rate = overrideRate;
        }

        try {
            final double finalRate = rate;
            RateLimiter rateLimiter = rateLimiterCache.get(key, () -> RateLimiter.create(finalRate));

            // 动态调整速率
            if (rateLimiter.getRate() != finalRate) {
                rateLimiter.setRate(finalRate);
            }

            if (timeout > 0) {
                return rateLimiter.tryAcquire(timeout, timeUnit);
            } else {
                return rateLimiter.tryAcquire();
            }
        } catch (Exception e) {
            log.error("Guava限流异常 - key: {}", key, e);
            return true;
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
