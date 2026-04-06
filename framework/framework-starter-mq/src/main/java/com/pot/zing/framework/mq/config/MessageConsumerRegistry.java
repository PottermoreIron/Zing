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
            log.info("[MQ] 未找到MessageConsumer，跳过消费者注册");
            return;
        }

        log.info("[MQ] 发现{}个MessageConsumer，开始注册", consumers.size());

        for (Map.Entry<String, MessageConsumer<?>> entry : consumers.entrySet()) {
            String beanName = entry.getKey();
            MessageConsumer<?> consumer = entry.getValue();

            try {
                registerConsumer(beanName, consumer);
            } catch (Exception e) {
                log.error("[MQ] 注册消费者失败: bean={}, error={}", beanName, e.getMessage(), e);
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
                    log.debug("[MQ] 收到消息: queue={}, message={}", queueName, message);
                    T event = objectMapper.readValue(message, messageType);
                    consumer.consume(event);
                } catch (Exception e) {
                    log.error("[MQ] 消息处理失败: queue={}, error={}", queueName, e.getMessage(), e);
                    throw new RuntimeException("Message processing failed", e);
                }
            }
        });

        container.setMessageListener(adapter);
        container.start();

        log.info("[MQ] 消费者注册成功: bean={}, queue={}, messageType={}",
                beanName, queueName, messageType.getSimpleName());
    }
}
