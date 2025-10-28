package com.pot.zing.framework.starter.ratelimit.service;

import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/10/18 22:01
 * @description: 自定义限流管理器接口
 */
public interface RateLimitManager {

    /**
     * 尝试获取令牌
     *
     * @param key      限流key
     * @param rate     速率（每秒请求数）
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    boolean tryAcquire(String key, double rate, long timeout, TimeUnit timeUnit);

    /**
     * 获取管理器类型
     *
     * @return 管理器类型标识
     */
    String getType();
}
