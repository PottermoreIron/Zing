package com.pot.zing.framework.starter.ratelimit.service;

import java.util.concurrent.TimeUnit;

/**
 * Contract for acquiring rate-limit tokens.
 */
public interface RateLimitManager {

    /**
     * Attempts to acquire a token.
     */
    boolean tryAcquire(String key, double rate, long timeout, TimeUnit timeUnit);

    /**
     * Returns the manager type identifier.
     */
    String getType();
}
