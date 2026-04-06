package com.pot.zing.framework.starter.redis.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for the Redis starter.
 */
@Data
@ConfigurationProperties(prefix = "pot.redis")
public class RedisProperties {

    /**
     * Enables the Redis starter.
     */
    private boolean enabled = true;

    /**
     * Global key prefix.
     */
    private String keyPrefix = "pot:";

    /**
     * Key segment separator.
     */
    private String keySeparator = ":";

    /**
     * Default expiration time in seconds.
     */
    private Long defaultExpireTime = 3600L;

    /**
     * Serialization settings.
     */
    private Serializer serializer = new Serializer();

    /**
     * Distributed lock settings.
     */
    private Lock lock = new Lock();

    /**
     * Cache settings.
     */
    private Cache cache = new Cache();

    public enum SerializerType {
        JSON, JDK, PROTOSTUFF
    }

    @Data
    public static class Serializer {
        /**
         * Serialization format.
         */
        private SerializerType type = SerializerType.JSON;

        /**
         * Enables polymorphic type metadata.
         */
        private boolean enableTyping = true;
    }

    @Data
    public static class Lock {
        /**
         * Lock key prefix.
         */
        private String prefix = "lock:";

        /**
         * Default wait time.
         */
        private Duration waitTime = Duration.ofSeconds(3);

        /**
         * Default lease time.
         */
        private Duration leaseTime = Duration.ofSeconds(30);

        /**
         * Watchdog timeout.
         */
        private Duration watchdogTimeout = Duration.ofSeconds(30);
    }

    @Data
    public static class Cache {
        /**
         * Cache key prefix.
         */
        private String prefix = "cache:";

        /**
         * Allows caching null values.
         */
        private boolean allowNullValues = true;

        /**
         * Cache TTL.
         */
        private Duration timeToLive = Duration.ofHours(1);
    }
}
