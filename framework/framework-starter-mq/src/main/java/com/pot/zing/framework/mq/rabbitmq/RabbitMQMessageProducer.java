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
 * RabbitMQ-backed message producer.
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
            String jsonMessage = objectMapper.writeValueAsString(message);

            Message amqpMessage = MessageBuilder
                    .withBody(jsonMessage.getBytes())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .build();

            rabbitTemplate.send(topic, routingKey, amqpMessage);

            log.debug("[RabbitMQ] Message published — exchange={}, routingKey={}, type={}",
                    topic, routingKey, message.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish message — exchange={}, routingKey={}", topic, routingKey, e);
            throw new RuntimeException("Failed to send message to RabbitMQ", e);
        }
    }

    @Override
    public void sendWithConfirm(String topic, Object message, PublishCallback callback) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            Message amqpMessage = MessageBuilder
                    .withBody(jsonMessage.getBytes())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .build();

            CorrelationData correlationData = new CorrelationData();
            correlationData.getFuture().whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[RabbitMQ] Publish confirmation failed — exchange={}", topic, ex);
                    callback.onFailure(ex);
                } else if (result != null && result.isAck()) {
                    log.debug("[RabbitMQ] Publish confirmed — exchange={}", topic);
                    callback.onSuccess();
                } else {
                    log.error("[RabbitMQ] Message rejected by broker — exchange={}, reason={}",
                            topic, result != null ? result.getReason() : "unknown");
                    callback.onFailure(new RuntimeException("Message rejected by broker"));
                }
            });

            rabbitTemplate.send(topic, "", amqpMessage, correlationData);

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish message — exchange={}", topic, e);
            callback.onFailure(e);
        }
    }
}
