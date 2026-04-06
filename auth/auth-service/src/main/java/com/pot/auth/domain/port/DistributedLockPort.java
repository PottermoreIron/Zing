package com.pot.auth.domain.port;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface DistributedLockPort {

        <T> T executeWithLock(
            String lockKey,
            long waitTime,
            long leaseTime,
            TimeUnit timeUnit,
            Supplier<T> task
    );

        boolean executeWithLock(
            String lockKey,
            long waitTime,
            long leaseTime,
            TimeUnit timeUnit,
            Runnable task
    );

        boolean tryLock(String lockKey, long leaseTime, TimeUnit timeUnit);

        void unlock(String lockKey);
}

