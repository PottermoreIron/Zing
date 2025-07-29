package com.pot.user.service.ratelimit;

import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/3/30 16:10
 * @description: 限流接口
 */
public interface RateLimitManager {
    /**
     * 尝试获取令牌
     *
     * @param key      限流key
     * @param rate     限流速率
     * @param timeout  等待超时时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    boolean tryAcquire(String key, double rate, long timeout, TimeUnit timeUnit);
}
