package com.pot.zing.framework.starter.touch.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the touch starter.
 */
@Data
@ConfigurationProperties(prefix = "pot.touch")
public class TouchProperties {

    /**
     * Enables touch delivery features.
     */
    private boolean enabled = true;

    /**
     * Runtime environment label.
     */
    private String env = "pre";

    /**
     * SMS settings.
     */
    private SmsConfig sms = new SmsConfig();

    /**
     * Email settings.
     */
    private EmailConfig email = new EmailConfig();

    /**
     * Rate-limit settings.
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    @Data
    public static class SmsConfig {
        /**
         * SMS provider, such as aliyun or tencent.
         */
        private String provider = "aliyun";

        /**
         * SMS sign name.
         */
        private String signName;
    }

    @Data
    public static class EmailConfig {
        /**
         * Email provider, such as spring, sendgrid, or aliyun.
         */
        private String provider = "spring";

        /**
         * Sender email address.
         */
        private String from = "noreply@yoursite.com";
    }

    @Data
    public static class RateLimitConfig {
        /**
         * Enables rate limiting.
         */
        private boolean enabled = true;

        /**
         * Maximum sends per minute.
         */
        private int maxPerMinute = 5;
    }
}
