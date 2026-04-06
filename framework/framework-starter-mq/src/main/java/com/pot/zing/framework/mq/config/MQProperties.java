package com.pot.zing.framework.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Message queue configuration properties.
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Data
@ConfigurationProperties(prefix = "pot.mq")
public class MQProperties {

    /**
     * Queue provider type.
     */
    private MQType type = MQType.RABBITMQ;

    /**
     * Optional prefix for generated topics.
     */
    private String topicPrefix = "pot.events";

    /**
     * Enables the MQ starter.
     */
    private boolean enabled = true;

    /**
     * RabbitMQ-specific settings.
     */
    private RabbitMQConfig rabbitmq = new RabbitMQConfig();

    /**
     * Kafka-specific settings.
     */
    private KafkaConfig kafka = new KafkaConfig();

    /**
     * Supported queue providers.
     */
    public enum MQType {
        RABBITMQ,
        KAFKA
    }

    /**
     * RabbitMQ configuration.
     */
    @Data
    public static class RabbitMQConfig {
        /**
         * Default exchange type.
         */
        private String exchangeType = "topic";

        /**
         * Enables publisher confirms.
         */
        private boolean publisherConfirms = true;

        /**
         * Enables publisher returns.
         */
        private boolean publisherReturns = true;

        /**
         * Message TTL in milliseconds. Use -1 to leave it unset.
         */
        private long messageTtl = 86_400_000L;

        /**
         * Dead-letter exchange.
         */
        private String deadLetterExchange = "pot.dlx";

        /**
         * Maximum retry attempts.
         */
        private int maxRetryAttempts = 3;
    }

    /**
     * Kafka configuration.
     */
    @Data
    public static class KafkaConfig {
        /**
         * Default partition count.
         */
        private int defaultPartitions = 3;

        /**
         * Default replication factor.
         */
        private short defaultReplicationFactor = 1;

        /**
         * Message retention in milliseconds.
         */
        private long retentionMs = 604_800_000L;
    }
}
