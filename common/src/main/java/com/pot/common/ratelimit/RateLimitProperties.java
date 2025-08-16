package com.pot.common.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/3/30 21:44
 * @description: 限流配置属性
 */
@Data
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    /**
     * 是否启用限流功能，默认启用
     */
    private boolean enabled = true;

    /**
     * 限流器缓存过期时间（小时）
     */
    private int expireAfterAccess = 1;

    /**
     * 限流key前缀
     */
    private String keyPrefix = "";

    /**
     * 全局速率因子，用于统一调整所有限流器的速率
     * 例如，设置为0.5将所有限流速率减半
     */
    private double globalRateFactor = 1.0;

    /**
     * 限流器实现提供者
     */
    private String provider = "guava";

    /**
     * 是否启用基于IP的限流
     */
    private boolean ipBasedEnabled = true;

    /**
     * 是否启用基于用户的限流
     */
    private boolean userBasedEnabled = true;

    /**
     * 针对特定key的速率覆盖
     * 可以在配置文件中覆盖注解中定义的速率
     */
    private Map<String, Double> rateOverrides = new HashMap<>();
}
