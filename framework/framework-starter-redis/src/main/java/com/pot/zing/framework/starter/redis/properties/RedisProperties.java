package com.pot.zing.framework.starter.redis.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author: Pot
 * @created: 2025/10/18 20:41
 * @description: Redis属性配置
 */
@Data
@ConfigurationProperties(prefix = "pot.redis")
public class RedisProperties {

    /**
     * 是否启用Redis
     */
    private boolean enabled = true;

    /**
     * 键前缀
     */
    private String keyPrefix = "pot:";

    /**
     * 键分隔符
     */
    private String keySeparator = ":";

    /**
     * 默认过期时间（秒）
     */
    private Long defaultExpireTime = 3600L;

    /**
     * 序列化配置
     */
    private Serializer serializer = new Serializer();

    /**
     * 分布式锁配置
     */
    private Lock lock = new Lock();

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    @Data
    public static class Serializer {
        /**
         * 序列化类型: JSON, JDK, PROTOSTUFF
         */
        private SerializerType type = SerializerType.JSON;

        /**
         * 是否启用类型信息
         */
        private boolean enableTyping = true;
    }

    @Data
    public static class Lock {
        /**
         * 锁前缀
         */
        private String prefix = "lock:";

        /**
         * 默认等待时间
         */
        private Duration waitTime = Duration.ofSeconds(3);

        /**
         * 默认租约时间
         */
        private Duration leaseTime = Duration.ofSeconds(30);

        /**
         * 看门狗超时时间
         */
        private Duration watchdogTimeout = Duration.ofSeconds(30);
    }

    @Data
    public static class Cache {
        /**
         * 缓存前缀
         */
        private String prefix = "cache:";

        /**
         * 是否允许空值
         */
        private boolean allowNullValues = true;

        /**
         * 缓存过期时间
         */
        private Duration timeToLive = Duration.ofHours(1);
    }

    public enum SerializerType {
        JSON, JDK, PROTOSTUFF
    }
}
