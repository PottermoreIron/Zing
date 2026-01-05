package com.pot.zing.framework.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQ配置属性
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Data
@ConfigurationProperties(prefix = "pot.mq")
public class MQProperties {

    /**
     * MQ类型：rabbitmq 或 kafka
     */
    private MQType type = MQType.RABBITMQ;

    /**
     * Topic前缀（可选）
     */
    private String topicPrefix = "pot.events";

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * RabbitMQ配置
     */
    private RabbitMQConfig rabbitmq = new RabbitMQConfig();

    /**
     * Kafka配置
     */
    private KafkaConfig kafka = new KafkaConfig();

    /**
     * MQ类型枚举
     */
    public enum MQType {
        RABBITMQ,
        KAFKA
    }

    /**
     * RabbitMQ配置
     */
    @Data
    public static class RabbitMQConfig {
        /**
         * 默认Exchange类型
         */
        private String exchangeType = "topic";

        /**
         * 是否启用发布确认
         */
        private boolean publisherConfirms = true;

        /**
         * 是否启用发布返回
         */
        private boolean publisherReturns = true;

        /**
         * 消息TTL（毫秒），-1表示不设置
         */
        private long messageTtl = 86400000; // 24小时

        /**
         * 死信Exchange
         */
        private String deadLetterExchange = "pot.dlx";

        /**
         * 最大重试次数
         */
        private int maxRetryAttempts = 3;
    }

    /**
     * Kafka配置
     */
    @Data
    public static class KafkaConfig {
        /**
         * 默认分区数
         */
        private int defaultPartitions = 3;

        /**
         * 默认副本数
         */
        private short defaultReplicationFactor = 1;

        /**
         * 消息保留时间（毫秒）
         */
        private long retentionMs = 604800000; // 7天
    }
}
