package com.pot.zing.framework.starter.ratelimit.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/10/18 22:03
 * @description: 自定义限流配置属性
 */
@Data
@ConfigurationProperties(prefix = "pot.ratelimit")
public class RateLimitProperties {

    /**
     * 是否启用限流
     */
    private boolean enabled = true;

    /**
     * 限流实现提供者：guava、redis
     */
    private String provider = "guava";

    /**
     * 全局key前缀
     */
    private String keyPrefix = "pot:ratelimit:";

    /**
     * 全局速率因子（可用于统一调整所有限流速率）
     */
    private double globalRateFactor = 1.0;

    /**
     * 缓存过期时间（小时）
     */
    private int expireAfterAccess = 1;

    /**
     * 是否启用IP限流
     */
    private boolean ipBasedEnabled = true;

    /**
     * 是否启用用户限流
     */
    private boolean userBasedEnabled = true;

    /**
     * 特定key的速率覆盖配置
     */
    private Map<String, Double> rateOverrides = new HashMap<>();

    /**
     * Redis配置（当provider为redis时生效）
     */
    private RedisConfig redis = new RedisConfig();

    @Data
    public static class RedisConfig {
        /**
         * 令牌桶容量系数（容量 = 速率 * 容量系数）
         */
        private double capacityFactor = 2.0;

        /**
         * 令牌key后缀
         */
        private String tokensSuffix = ":tokens";

        /**
         * 最后填充时间key后缀
         */
        private String lastRefillSuffix = ":last_refill";
    }
}
