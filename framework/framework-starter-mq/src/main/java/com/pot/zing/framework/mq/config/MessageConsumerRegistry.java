package com.pot.zing.framework.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.zing.framework.mq.core.MessageConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Registers discovered message consumers with RabbitMQ listeners.
 *
 * @author Copilot
 * @since 2026-01-06
 */
@Slf4j
@Configuration
@ConditionalOnClass(ConnectionFactory.class)
@RequiredArgsConstructor
public class MessageConsumerRegistry {

    private final ApplicationContext applicationContext;
    private final ConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;

    @PostConstruct
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void registerConsumers() {
        Map<String, MessageConsumer<?>> consumers = (Map<String, MessageConsumer<?>>) (Map<String, ? extends MessageConsumer>) applicationContext
                .getBeansOfType(MessageConsumer.class);

        if (consumers.isEmpty()) {
            log.info("[MQ] No MessageConsumer beans found, skipping consumer registration");
            return;
        }

        log.info("[MQ] Discovered {} MessageConsumer bean(s), starting registration", consumers.size());

        for (Map.Entry<String, MessageConsumer<?>> entry : consumers.entrySet()) {
            String beanName = entry.getKey();
            MessageConsumer<?> consumer = entry.getValue();

            try {
                registerConsumer(beanName, consumer);
            } catch (Exception e) {
                log.error("[MQ] Failed to register consumer — bean={}, error={}", beanName, e.getMessage(), e);
            }
        }
    }

    private <T> void registerConsumer(String beanName, MessageConsumer<T> consumer) {
        String queueName = consumer.getQueue();
        Class<T> messageType = consumer.getMessageType();

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);

        MessageListenerAdapter adapter = new MessageListenerAdapter(new Object() {
            @SuppressWarnings("unused")
            public void handleMessage(String message) {
                try {
                    log.debug("[MQ] Message received — queue={}, message={}", queueName, message);
                    T event = objectMapper.readValue(message, messageType);
                    consumer.consume(event);
                } catch (Exception e) {
                    log.error("[MQ] Message processing failed — queue={}, error={}", queueName, e.getMessage(), e);
                    throw new RuntimeException("Message processing failed", e);
                }
            }
        });

        container.setMessageListener(adapter);
        container.start();

        log.info("[MQ] Consumer registered — bean={}, queue={}, messageType={}",
                beanName, queueName, messageType.getSimpleName());
    }
}
