package com.pot.zing.framework.starter.touch.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for verification-code delivery.
 */
@Data
@ConfigurationProperties(prefix = "pot.touch.verification-code")
public class VerificationCodeProperties {

    public static final String CODE_KEY_PREFIX = "verification_code";
    public static final String RATE_LIMIT_KEY_PREFIX = "verification_rate";
    public static final String FAILURE_KEY_PREFIX = "verification_failure";
    /**
     * Default verification code length.
     */
    private Integer defaultCodeLength = 6;

    /**
     * Default expiration time in seconds.
     */
    private Long defaultExpireSeconds = 300L;

    /**
     * Minimum resend interval in seconds.
     */
    private Long rateLimitSeconds = 60L;

    /**
     * Maximum verification failures before lockout.
     */
    private Integer maxFailureCount = 5;

    /**
     * Failure record expiration time in minutes.
     */
    private Long failureExpireMinutes = 30L;
}
