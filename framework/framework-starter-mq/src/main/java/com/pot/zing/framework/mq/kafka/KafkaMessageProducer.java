package com.pot.zing.framework.mq.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.zing.framework.mq.core.MessageProducer;
import com.pot.zing.framework.mq.core.PublishCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka消息生产者实现
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaMessageProducer implements MessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void send(String topic, Object message) {
        send(topic, null, message);
    }

    @Override
    public void send(String topic, String routingKey, Object message) {
        try {
            // 序列化消息
            String jsonMessage = objectMapper.writeValueAsString(message);

            // 发送消息（routingKey作为partition key）
            if (routingKey != null && !routingKey.isEmpty()) {
                kafkaTemplate.send(topic, routingKey, jsonMessage);
            } else {
                kafkaTemplate.send(topic, jsonMessage);
            }

            log.debug("[Kafka] 消息发送成功: topic={}, partitionKey={}, message={}",
                    topic, routingKey, message.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("[Kafka] 消息发送失败: topic={}, partitionKey={}", topic, routingKey, e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }

    @Override
    public void sendWithConfirm(String topic, Object message, PublishCallback callback) {
        try {
            // 序列化消息
            String jsonMessage = objectMapper.writeValueAsString(message);

            // 发送消息并处理回调
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, jsonMessage);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[Kafka] 消息发送确认失败: topic={}", topic, ex);
                    callback.onFailure(ex);
                } else {
                    log.debug("[Kafka] 消息发送确认成功: topic={}, partition={}, offset={}",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    callback.onSuccess();
                }
            });

        } catch (Exception e) {
            log.error("[Kafka] 消息发送失败: topic={}", topic, e);
            callback.onFailure(e);
        }
    }
}
