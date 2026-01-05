package com.pot.zing.framework.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.zing.framework.mq.core.MessageConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * 消息消费者注册器
 * 
 * 自动扫描所有MessageConsumer bean并注册到RabbitMQ监听器
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
    public void registerConsumers() {
        Map<String, MessageConsumer> consumers = applicationContext.getBeansOfType(MessageConsumer.class);
        
        if (consumers.isEmpty()) {
            log.info("[MQ] 未找到MessageConsumer，跳过消费者注册");
            return;
        }
        
        log.info("[MQ] 发现{}个MessageConsumer，开始注册", consumers.size());
        
        for (Map.Entry<String, MessageConsumer> entry : consumers.entrySet()) {
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
        
        // 创建队列（如果不存在）
        Queue queue = new Queue(queueName, true, false, false);
        
        // 创建消息监听容器
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        
        // 创建消息监听适配器
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
