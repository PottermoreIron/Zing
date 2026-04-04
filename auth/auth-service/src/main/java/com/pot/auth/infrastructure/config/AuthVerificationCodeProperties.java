package com.pot.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码业务策略配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.verification-code")
public class AuthVerificationCodeProperties {

    private String codeKeyPrefix = "auth:code:";
    private String attemptsKeyPrefix = "auth:code:attempts:";
    private String sendLimitKeyPrefix = "auth:code:send:";
    private long ttlSeconds = 300;
    private int maxAttempts = 3;
    private long sendCooldownSeconds = 60;
    private Lock lock = new Lock();

    @Data
    public static class Lock {
        private long waitSeconds = 3;
        private long leaseSeconds = 10;
    }
}