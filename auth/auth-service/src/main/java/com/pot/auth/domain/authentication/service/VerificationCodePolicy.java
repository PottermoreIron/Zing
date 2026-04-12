package com.pot.auth.domain.authentication.service;

import java.time.Duration;

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
            throw new IllegalArgumentException("Verification code TTL must be greater than 0");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max verification attempts must be greater than 0");
        }
        if (sendCooldownSeconds <= 0) {
            throw new IllegalArgumentException("Verification code send cooldown must be greater than 0");
        }
        if (lockWaitSeconds <= 0 || lockLeaseSeconds <= 0) {
            throw new IllegalArgumentException("Verification code distributed lock parameter must be greater than 0");
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