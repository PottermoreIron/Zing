package com.pot.zing.framework.mq.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.zing.framework.mq.core.MessageProducer;
import com.pot.zing.framework.mq.core.PublishCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ消息生产者实现
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitMQMessageProducer implements MessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void send(String topic, Object message) {
        send(topic, "", message);
    }

    @Override
    public void send(String topic, String routingKey, Object message) {
        try {
            // 序列化消息
            String jsonMessage = objectMapper.writeValueAsString(message);

            // 构建消息
            Message amqpMessage = MessageBuilder
                    .withBody(jsonMessage.getBytes())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .build();

            // 发送消息
            rabbitTemplate.send(topic, routingKey, amqpMessage);

            log.debug("[RabbitMQ] 消息发送成功: exchange={}, routingKey={}, message={}",
                    topic, routingKey, message.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("[RabbitMQ] 消息发送失败: exchange={}, routingKey={}", topic, routingKey, e);
            throw new RuntimeException("Failed to send message to RabbitMQ", e);
        }
    }

    @Override
    public void sendWithConfirm(String topic, Object message, PublishCallback callback) {
        try {
            // 序列化消息
            String jsonMessage = objectMapper.writeValueAsString(message);

            // 构建消息
            Message amqpMessage = MessageBuilder
                    .withBody(jsonMessage.getBytes())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .build();

            // 创建关联数据
            CorrelationData correlationData = new CorrelationData();
            correlationData.getFuture().whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[RabbitMQ] 消息发送确认失败: exchange={}", topic, ex);
                    callback.onFailure(ex);
                } else if (result != null && result.isAck()) {
                    log.debug("[RabbitMQ] 消息发送确认成功: exchange={}", topic);
                    callback.onSuccess();
                } else {
                    log.error("[RabbitMQ] 消息发送被broker拒绝: exchange={}, reason={}",
                            topic, result != null ? result.getReason() : "unknown");
                    callback.onFailure(new RuntimeException("Message rejected by broker"));
                }
            });

            // 发送消息
            rabbitTemplate.send(topic, "", amqpMessage, correlationData);

        } catch (Exception e) {
            log.error("[RabbitMQ] 消息发送失败: exchange={}", topic, e);
            callback.onFailure(e);
        }
    }
}
