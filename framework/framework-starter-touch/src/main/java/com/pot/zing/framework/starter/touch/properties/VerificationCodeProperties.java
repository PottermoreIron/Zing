package com.pot.zing.framework.starter.touch.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: Pot
 * @created: 2025/10/19 16:56
 * @description: 验证码配置
 */
@Data
@ConfigurationProperties(prefix = "pot.touch.verification-code")
public class VerificationCodeProperties {
    
    public static final String CODE_KEY_PREFIX = "verification_code";
    public static final String RATE_LIMIT_KEY_PREFIX = "verification_rate";
    public static final String FAILURE_KEY_PREFIX = "verification_failure";
    /**
     * 默认验证码长度
     */
    private Integer defaultCodeLength = 6;

    /**
     * 默认过期时间(秒)
     */
    private Long defaultExpireSeconds = 300L;

    /**
     * 发送频率限制(秒)
     */
    private Long rateLimitSeconds = 60L;

    /**
     * 最大验证失败次数
     */
    private Integer maxFailureCount = 5;

    /**
     * 失败记录过期时间(分钟)
     */
    private Long failureExpireMinutes = 30L;
}
