package com.pot.auth.domain.port;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁端口接口（防腐层）
 *
 * <p>隔离分布式锁的具体实现（Redis、Zookeeper等）
 *
 * <p>设计原则：
 * <ul>
 *   <li>领域层定义接口（Port）</li>
 *   <li>基础设施层实现适配器（RedisLockAdapter）</li>
 *   <li>支持自动释放和重试机制</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-10
 */
public interface DistributedLockPort {

    /**
     * 尝试获取锁并执行任务
     *
     * @param lockKey   锁的key
     * @param waitTime  等待时间
     * @param leaseTime 锁自动释放时间
     * @param timeUnit  时间单位
     * @param task      要执行的任务
     * @param <T>       返回值类型
     * @return 任务执行结果，如果获取锁失败返回null
     */
    <T> T executeWithLock(
            String lockKey,
            long waitTime,
            long leaseTime,
            TimeUnit timeUnit,
            Supplier<T> task
    );

    /**
     * 尝试获取锁并执行任务（无返回值）
     *
     * @param lockKey   锁的key
     * @param waitTime  等待时间
     * @param leaseTime 锁自动释放时间
     * @param timeUnit  时间单位
     * @param task      要执行的任务
     * @return 是否成功执行（获取锁失败返回false）
     */
    boolean executeWithLock(
            String lockKey,
            long waitTime,
            long leaseTime,
            TimeUnit timeUnit,
            Runnable task
    );

    /**
     * 尝试获取锁（需要手动释放）
     *
     * @param lockKey   锁的key
     * @param leaseTime 锁自动释放时间
     * @param timeUnit  时间单位
     * @return 是否成功获取锁
     */
    boolean tryLock(String lockKey, long leaseTime, TimeUnit timeUnit);

    /**
     * 释放锁
     *
     * @param lockKey 锁的key
     */
    void unlock(String lockKey);
}

