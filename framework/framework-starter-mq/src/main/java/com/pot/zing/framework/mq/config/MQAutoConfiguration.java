package com.pot.zing.framework.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.zing.framework.mq.core.MessageProducer;
import com.pot.zing.framework.mq.core.MessageTemplate;
import com.pot.zing.framework.mq.kafka.KafkaMessageProducer;
import com.pot.zing.framework.mq.rabbitmq.RabbitMQMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Auto-configuration for MQ infrastructure beans.
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Slf4j
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@EnableConfigurationProperties(MQProperties.class)
@ConditionalOnProperty(prefix = "pot.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MQAutoConfiguration {

    /**
     * RabbitMQ-specific bean configuration.
     */
    @Configuration
    @ConditionalOnClass(RabbitTemplate.class)
    @ConditionalOnProperty(prefix = "pot.mq", name = "type", havingValue = "rabbitmq", matchIfMissing = true)
    static class RabbitMQConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MQProperties mqProperties) {
            RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

            if (mqProperties.getRabbitmq().isPublisherConfirms()) {
                rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                    if (ack) {
                        log.debug("[RabbitMQ] 消息发送确认: correlationData={}", correlationData);
                    } else {
                        log.error("[RabbitMQ] 消息发送失败: correlationData={}, cause={}", correlationData, cause);
                    }
                });
            }

            if (mqProperties.getRabbitmq().isPublisherReturns()) {
                rabbitTemplate.setMandatory(true);
                rabbitTemplate.setReturnsCallback(returned -> {
                    log.warn("[RabbitMQ] 消息无法路由: message={}, replyCode={}, replyText={}, exchange={}, routingKey={}",
                            returned.getMessage(),
                            returned.getReplyCode(),
                            returned.getReplyText(),
                            returned.getExchange(),
                            returned.getRoutingKey());
                });
            }

            log.info("[MQ] RabbitTemplate配置完成");
            return rabbitTemplate;
        }

        @Bean
        @ConditionalOnMissingBean
        public MessageProducer rabbitMQMessageProducer(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
            log.info("[MQ] 使用RabbitMQ作为消息队列");
            return new RabbitMQMessageProducer(rabbitTemplate, objectMapper);
        }
    }

    /**
     * Kafka-specific bean configuration.
     */
    @Configuration
    @ConditionalOnClass(KafkaTemplate.class)
    @ConditionalOnProperty(prefix = "pot.mq", name = "type", havingValue = "kafka")
    static class KafkaConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public MessageProducer kafkaMessageProducer(KafkaTemplate<String, String> kafkaTemplate,
                ObjectMapper objectMapper) {
            log.info("[MQ] 使用Kafka作为消息队列");
            return new KafkaMessageProducer(kafkaTemplate, objectMapper);
        }
    }

    /**
     * Shared message template.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MessageProducer.class)
    public MessageTemplate messageTemplate(MessageProducer messageProducer) {
        log.info("[MQ] MessageTemplate配置完成");
        return new MessageTemplate(messageProducer);
    }

    /**
     * Fallback object mapper.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
