package com.pot.zing.framework.starter.ratelimit.service.impl;

import com.pot.zing.framework.starter.ratelimit.properties.RateLimitProperties;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitManager;
import com.pot.zing.framework.starter.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/10/18 22:04
 * @description: 自定义Redis限流管理器实现
 */
@Slf4j
public class RedisRateLimitManager implements RateLimitManager {

    private static final String TYPE = "redis";
    private static final String LUA_SCRIPT_PATH = "META-INF/scripts/ratelimit.lua";

    private final RedisService redisService;
    private final RateLimitProperties properties;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RedisRateLimitManager(RedisService redisService, RateLimitProperties properties) {
        this.redisService = redisService;
        this.properties = properties;
        this.rateLimitScript = createRateLimitScript();
    }

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit timeUnit) {
        log.debug("Redis限流尝试获取令牌 - key: {}, rate: {}, timeout: {}", key, rate, timeout);

        // 应用全局速率因子
        rate = rate * properties.getGlobalRateFactor();

        // 检查速率覆盖配置
        Double overrideRate = properties.getRateOverrides().get(key);
        if (overrideRate != null) {
            rate = overrideRate;
        }

        String tokensKey = key + properties.getRedis().getTokensSuffix();
        String lastRefillKey = key + properties.getRedis().getLastRefillSuffix();
        double capacity = rate * properties.getRedis().getCapacityFactor();
        long now = System.currentTimeMillis() / 1000;
        int ttl = properties.getExpireAfterAccess() * 3600;

        try {
            if (timeout > 0) {
                return tryAcquireWithTimeout(key, tokensKey, lastRefillKey, rate, capacity, now, ttl, timeout, timeUnit);
            } else {
                return tryAcquireImmediately(key, tokensKey, lastRefillKey, rate, capacity, now, ttl);
            }
        } catch (Exception e) {
            log.error("Redis限流异常 - key: {}", key, e);
            // 异常时放行，避免影响业务
            return true;
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * 立即尝试获取令牌
     */
    private boolean tryAcquireImmediately(String key, String tokensKey, String lastRefillKey,
                                          double rate, double capacity, long now, int ttl) {
        List<String> keys = Arrays.asList(key, tokensKey, lastRefillKey);
        Long result = executeScript(keys, rate, capacity, now, 1, ttl);
        return result != null && result == 1;
    }

    /**
     * 带超时的令牌获取
     */
    private boolean tryAcquireWithTimeout(String key, String tokensKey, String lastRefillKey,
                                          double rate, double capacity, long now, int ttl,
                                          long timeout, TimeUnit timeUnit) throws InterruptedException {
        long endTime = System.nanoTime() + timeUnit.toNanos(timeout);
        List<String> keys = Arrays.asList(key, tokensKey, lastRefillKey);

        while (System.nanoTime() < endTime) {
            Long result = executeScript(keys, rate, capacity, now, 1, ttl);
            if (result != null && result == 1) {
                return true;
            }

            // 短暂休眠后重试
            TimeUnit.MILLISECONDS.sleep(50);
        }

        return false;
    }

    /**
     * 执行Lua脚本
     */
    private Long executeScript(List<String> keys, double rate, double capacity,
                               long now, int requested, int ttl) {
        try {
            return redisService.execute(
                    rateLimitScript,
                    keys,
                    rate, capacity, now, requested, ttl
            );
        } catch (Exception e) {
            log.error("执行限流Lua脚本失败 - keys: {}", keys, e);
            // 脚本执行失败时放行，避免影响业务
            return 1L;
        }
    }

    /**
     * 创建限流Lua脚本
     */
    private DefaultRedisScript<Long> createRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        try {
            script.setScriptSource(new ResourceScriptSource(
                    new ClassPathResource(LUA_SCRIPT_PATH)
            ));
            script.setResultType(Long.class);
            log.info("限流Lua脚本加载成功: {}", LUA_SCRIPT_PATH);
        } catch (Exception e) {
            log.error("限流Lua脚本加载失败: {}", LUA_SCRIPT_PATH, e);
            throw new IllegalStateException("Failed to load rate limit script", e);
        }
        return script;
    }
}
