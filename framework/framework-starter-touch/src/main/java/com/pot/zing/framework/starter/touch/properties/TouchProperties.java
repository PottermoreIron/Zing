package com.pot.zing.framework.starter.touch.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: Pot
 * @created: 2025/10/19 15:59
 * @description: 触达配置属性
 */
@Data
@ConfigurationProperties(prefix = "pot.touch")
public class TouchProperties {
    /**
     * 是否启用触达功能
     */
    private boolean enabled = true;
    /**
     *
     */
    private String env = "pre";

    /**
     * 短信配置
     */
    private SmsConfig sms = new SmsConfig();

    /**
     * 邮件配置
     */
    private EmailConfig email = new EmailConfig();

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    @Data
    public static class SmsConfig {
        /**
         * 短信服务提供商: aliyun, tencent
         */
        private String provider = "aliyun";

        /**
         * 短信签名
         */
        private String signName;
    }

    @Data
    public static class EmailConfig {
        /**
         * 邮件服务提供商: spring, sendgrid, aliyun
         */
        private String provider = "spring";

        /**
         * 发件人邮箱
         */
        private String from = "noreply@yoursite.com";
    }

    @Data
    public static class RateLimitConfig {
        /**
         * 是否启用限流
         */
        private boolean enabled = true;

        /**
         * 每分钟最大发送次数
         */
        private int maxPerMinute = 5;
    }
}
