package com.pot.auth.infrastructure.adapter.lock;

import com.pot.auth.domain.port.DistributedLockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed lock adapter backed by Redis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockAdapter implements DistributedLockPort {

    private final StringRedisTemplate redisTemplate;

    @Override
    public <T> T executeWithLock(
            String lockKey,
            long waitTime,
            long leaseTime,
            TimeUnit timeUnit,
            Supplier<T> task) {
        String fullLockKey = "lock:" + lockKey;
        boolean locked = false;

        try {
            locked = tryLock(fullLockKey, leaseTime, timeUnit);

            if (!locked) {
                log.warn("[分布式锁] 获取锁失败: key={}", lockKey);
                return null;
            }

            return task.get();
        } finally {
            if (locked) {
                unlock(fullLockKey);
            }
        }
    }

    @Override
    public boolean executeWithLock(
            String lockKey,
            long waitTime,
            long leaseTime,
            TimeUnit timeUnit,
            Runnable task) {
        String fullLockKey = "lock:" + lockKey;
        boolean locked = false;

        try {
            locked = tryLock(fullLockKey, leaseTime, timeUnit);

            if (!locked) {
                log.warn("[分布式锁] 获取锁失败: key={}", lockKey);
                return false;
            }

            task.run();
            return true;
        } finally {
            if (locked) {
                unlock(fullLockKey);
            }
        }
    }

    @Override
    public boolean tryLock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", leaseTime, timeUnit);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("[分布式锁] 获取锁异常: key={}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.error("[分布式锁] 释放锁异常: key={}", lockKey, e);
        }
    }
}
