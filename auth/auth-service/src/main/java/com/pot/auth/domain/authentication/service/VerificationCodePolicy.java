package com.pot.auth.domain.authentication.service;

import java.time.Duration;

/**
 * 验证码策略对象，承载发送与校验相关的业务参数。
 */
public record VerificationCodePolicy(
        String codeKeyPrefix,
        String attemptsKeyPrefix,
        String sendLimitKeyPrefix,
        long ttlSeconds,
        int maxAttempts,
        long sendCooldownSeconds,
        long lockWaitSeconds,
        long lockLeaseSeconds) {

    public VerificationCodePolicy {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("验证码TTL必须大于0");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("验证码最大尝试次数必须大于0");
        }
        if (sendCooldownSeconds <= 0) {
            throw new IllegalArgumentException("验证码发送冷却时间必须大于0");
        }
        if (lockWaitSeconds <= 0 || lockLeaseSeconds <= 0) {
            throw new IllegalArgumentException("验证码分布式锁参数必须大于0");
        }
    }

    public Duration codeTtl() {
        return Duration.ofSeconds(ttlSeconds);
    }

    public Duration sendCooldown() {
        return Duration.ofSeconds(sendCooldownSeconds);
    }

    public String codeKey(String recipient) {
        return codeKeyPrefix + recipient;
    }

    public String attemptsKey(String recipient) {
        return attemptsKeyPrefix + recipient;
    }

    public String sendLimitKey(String recipient) {
        return sendLimitKeyPrefix + recipient;
    }

    public String lockKey(String recipient) {
        return "lock:send:code:" + recipient;
    }
}