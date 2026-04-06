package com.pot.zing.framework.starter.ratelimit.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the rate-limit starter.
 */
@Data
@ConfigurationProperties(prefix = "pot.ratelimit")
public class RateLimitProperties {

    /**
     * Enables rate limiting.
     */
    private boolean enabled = true;

    /**
     * Backing provider. Supported values are guava and redis.
     */
    private String provider = "guava";

    /**
     * Prefix applied to generated keys.
     */
    private String keyPrefix = "pot:ratelimit:";

    /**
     * Multiplier applied to all configured rates.
     */
    private double globalRateFactor = 1.0;

    /**
     * Expiration window in hours for cached limiters.
     */
    private int expireAfterAccess = 1;

    /**
     * Enables IP-based keys.
     */
    private boolean ipBasedEnabled = true;

    /**
     * Enables user-based keys.
     */
    private boolean userBasedEnabled = true;

    /**
     * Per-key rate overrides.
     */
    private Map<String, Double> rateOverrides = new HashMap<>();

    /**
     * Redis-specific settings.
     */
    private RedisConfig redis = new RedisConfig();

    @Data
    public static class RedisConfig {
        /**
         * Token bucket capacity multiplier.
         */
        private double capacityFactor = 2.0;

        /**
         * Token count suffix.
         */
        private String tokensSuffix = ":tokens";

        /**
         * Last refill timestamp suffix.
         */
        private String lastRefillSuffix = ":last_refill";
    }
}
