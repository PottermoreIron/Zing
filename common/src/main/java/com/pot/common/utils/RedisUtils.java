package com.pot.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author: Pot
 * @created: 2025/3/16 22:41
 * @description: Redis工具类
 */
@Slf4j
@Component
public class RedisUtils {
    private static RedisTemplate<Object, Object> redisTemplate;
    private static ValueOperations<Object, Object> valueOps;
    private static HashOperations<Object, Object, Object> hashOps;
    private static ListOperations<Object, Object> listOps;
    private static SetOperations<Object, Object> setOps;
    private static ZSetOperations<Object, Object> zSetOps;

    private static final String LOCK_PREFIX = "lock:";
    /**
     * 默认过期时间
     */
    private static final long DEFAULT_EXPIRE = 180;
    /**
     * 默认锁超时时间（秒）
     */
    private static final int DEFAULT_LOCK_TIMEOUT = 30;
    /**
     * 释放分布锁Lua脚本
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    @Autowired
    public RedisUtils(RedisTemplate<Object, Object> redisTemplate) {
        RedisUtils.redisTemplate = redisTemplate;
        RedisUtils.valueOps = redisTemplate.opsForValue();
        RedisUtils.hashOps = redisTemplate.opsForHash();
        RedisUtils.listOps = redisTemplate.opsForList();
        RedisUtils.setOps = redisTemplate.opsForSet();
        RedisUtils.zSetOps = redisTemplate.opsForZSet();
    }


    // ============================== Common ==============================

    /**
     * 删除键
     *
     * @param key 可以传单个或多个
     */
    public static boolean delete(Object... key) {
        if (key == null || key.length == 0) return false;
        try {
            Long count = redisTemplate.delete(Arrays.asList(key));
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Redis delete error", e);
            return false;
        }
    }

    /**
     * 设置过期时间
     *
     * @param time 时间（秒）
     */
    public static boolean expire(String key, long time) {
        try {
            if (time > 0) {
                return Boolean.TRUE.equals(redisTemplate.expire(key, time, TimeUnit.SECONDS));
            }
            return false;
        } catch (Exception e) {
            log.error("Redis expire error", e);
            return false;
        }
    }

    // ============================== String ==============================

    /**
     * 普通缓存获取
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Object key) {
        try {
            return key == null ? null : (T) valueOps.get(key);
        } catch (Exception e) {
            log.error("Redis get error", e);
            return null;
        }
    }

    /**
     * 带过期时间的缓存放入
     */
    public static boolean set(Object key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                valueOps.set(key, value, time, timeUnit);
            } else {
                valueOps.set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("Redis set error", e);
            return false;
        }
    }

    /**
     * 带过期时间的缓存放入
     */
    public static boolean set(Object key, Object value, long time) {
        try {
            return set(key, value, time, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis set error", e);
            return false;
        }
    }

    public static boolean set(Object key, Object value) {
        try {
            return set(key, value, DEFAULT_EXPIRE);
        } catch (Exception e) {
            log.error("Redis set error", e);
            return false;
        }
    }

    // ============================== Hash ==============================

    /**
     * 获取hash表字段值
     */
    @SuppressWarnings("unchecked")
    public static <T> T hget(Object key, Object field) {
        try {
            return (T) hashOps.get(key, field);
        } catch (Exception e) {
            log.error("Redis hget error", e);
            return null;
        }
    }

    /**
     * 设置hash表字段值
     */
    public static boolean hset(Object key, Object field, Object value) {
        try {
            hashOps.put(key, field, value);
            return true;
        } catch (Exception e) {
            log.error("Redis hset error", e);
            return false;
        }
    }

    // ============================== 分布式锁 ==============================

    /**
     * 获取分布式锁（简化版）
     *
     * @param lockKey   锁key
     * @param requestId 请求标识（用于释放锁验证）
     * @return 是否获取成功
     */
    public static boolean tryLock(String lockKey, String requestId) {
        return tryLock(lockKey, requestId, DEFAULT_LOCK_TIMEOUT);
    }

    public static boolean tryLock(String lockKey, String requestId, int expireSeconds) {
        String key = LOCK_PREFIX + lockKey;
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(
                            key,
                            requestId,
                            expireSeconds,
                            TimeUnit.SECONDS
                    )
            );
        } catch (Exception e) {
            log.error("Redis lock error", e);
            return false;
        }
    }

    /**
     * 释放分布式锁（Lua脚本保证原子性）
     */
    public static boolean releaseLock(String lockKey, String requestId) {
        String key = LOCK_PREFIX + lockKey;
        try {
            Long result = redisTemplate.execute(
                    UNLOCK_SCRIPT,
                    Collections.singletonList(key),
                    requestId
            );
            return result == 1;
        } catch (Exception e) {
            log.error("Redis unlock error", e);
            return false;
        }
    }
}
