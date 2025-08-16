package com.pot.common.ratelimit.impl;

import com.pot.common.ratelimit.RateLimitManager;
import com.pot.common.ratelimit.RateLimitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/3/31 23:21
 * @description: Redis限流管理
 */
@Slf4j
public class RedisRateLimitManager implements RateLimitManager {

    private final RedisTemplate<Object, Object> redisTemplate;
    private final RateLimitProperties properties;
    private static final String RATE_LIMITER_PREFIX = "rate:limiter:";
    private static final String TOKENS_KEY = ":tokens";
    private static final String LAST_REFILL_KEY = ":last_refill";

    // 令牌桶限流的Lua脚本，保证原子性
    private static final RedisScript<Long> RATE_LIMITER_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1] " +
                    "local tokensKey = KEYS[2] " +
                    "local lastRefillKey = KEYS[3] " +
                    "local rate = tonumber(ARGV[1]) " +
                    "local capacity = tonumber(ARGV[2]) " +
                    "local now = tonumber(ARGV[3]) " +
                    "local requested = tonumber(ARGV[4]) " +
                    "local ttl = tonumber(ARGV[5]) " +
                    "local lastRefill = tonumber(redis.call('get', lastRefillKey) or '0') " +
                    "local delta = math.max(0, now - lastRefill) " +
                    "local filled_tokens = math.min(capacity, tonumber(redis.call('get', tokensKey) or '0') + (delta * rate)) " +
                    "if filled_tokens >= requested then " +
                    "  redis.call('setex', tokensKey, ttl, filled_tokens - requested) " +
                    "  redis.call('setex', lastRefillKey, ttl, now) " +
                    "  return 1 " +
                    "else " +
                    "  redis.call('setex', tokensKey, ttl, filled_tokens) " +
                    "  redis.call('setex', lastRefillKey, ttl, now) " +
                    "  return 0 " +
                    "end",
            Long.class
    );

    public RedisRateLimitManager(RedisTemplate<Object, Object> redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit timeUnit) {
        log.info("Trying to acquire RateLimitManager for key: {} with rate: {}", key, rate);
        // 应用全局速率因子
        rate = rate * properties.getGlobalRateFactor();

        // 检查是否有针对该key的速率覆盖
        if (properties.getRateOverrides().containsKey(key)) {
            rate = properties.getRateOverrides().get(key);
        }

        // 应用key前缀
        String prefixedKey = properties.getKeyPrefix() + RATE_LIMITER_PREFIX + key;
        String tokensKey = prefixedKey + TOKENS_KEY;
        String lastRefillKey = prefixedKey + LAST_REFILL_KEY;

        // 令牌桶容量，默认为速率的2倍
        double capacity = rate * 2;

        // 当前时间戳（秒）
        long now = System.currentTimeMillis() / 1000;

        // 默认请求1个令牌
        int requested = 1;

        // 过期时间（秒）
        int ttl = properties.getExpireAfterAccess() * 3600;

        try {
            if (timeout > 0) {
                // 如果设置了超时，我们尝试等待获取令牌
                long endTime = System.nanoTime() + timeUnit.toNanos(timeout);
                while (true) {
                    Long result = redisTemplate.execute(
                            RATE_LIMITER_SCRIPT,
                            Arrays.asList(prefixedKey, tokensKey, lastRefillKey),
                            rate, capacity, now, requested, ttl
                    );

                    if (result == 1) {
                        return true;
                    }

                    // 检查是否超时
                    if (System.nanoTime() > endTime) {
                        return false;
                    }

                    // 短暂休眠后重试
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            } else {
                // 不等待，直接尝试获取令牌
                Long result = redisTemplate.execute(
                        RATE_LIMITER_SCRIPT,
                        Arrays.asList(prefixedKey, tokensKey, lastRefillKey),
                        rate, capacity, now, requested, ttl
                );

                return result == 1;
            }
        } catch (Exception e) {
            log.error("Redis rate limiter error for key: {}", key, e);
            // 发生错误时，默认放行以避免阻塞正常业务
            return true;
        }
    }
}
