package com.pot.zing.framework.mq.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息模板类（类似RedisTemplate）
 *
 * <p>
 * 提供更高层次的消息操作API
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Slf4j
@RequiredArgsConstructor
public class MessageTemplate {

    private final MessageProducer messageProducer;

    /**
     * 发送领域事件
     *
     * @param event 领域事件
     */
    public void publishDomainEvent(DomainEvent event) {
        String topic = buildTopicName(event);
        String routingKey = buildRoutingKey(event);
        
        log.info("[MQ] 发布领域事件: topic={}, routingKey={}, eventType={}", 
                topic, routingKey, event.getClass().getSimpleName());
        
        messageProducer.send(topic, routingKey, event);
    }

    /**
     * 发送领域事件（带确认）
     *
     * @param event    领域事件
     * @param callback 确认回调
     */
    public void publishDomainEventWithConfirm(DomainEvent event, PublishCallback callback) {
        String topic = buildTopicName(event);
        
        log.info("[MQ] 发布领域事件（带确认）: topic={}, eventType={}", 
                topic, event.getClass().getSimpleName());
        
        messageProducer.sendWithConfirm(topic, event, callback);
    }

    /**
     * 发送普通消息
     *
     * @param topic   主题
     * @param message 消息
     */
    public void send(String topic, Object message) {
        log.debug("[MQ] 发送消息: topic={}, messageType={}", topic, message.getClass().getSimpleName());
        messageProducer.send(topic, message);
    }

    /**
     * 发送普通消息（带路由键）
     *
     * @param topic      主题
     * @param routingKey 路由键
     * @param message    消息
     */
    public void send(String topic, String routingKey, Object message) {
        log.debug("[MQ] 发送消息: topic={}, routingKey={}, messageType={}", 
                topic, routingKey, message.getClass().getSimpleName());
        messageProducer.send(topic, routingKey, message);
    }

    /**
     * 构建Topic名称
     * 格式: {domain}.{event}.{version}
     * 例如: member.permission.changed.v1
     */
    private String buildTopicName(DomainEvent event) {
        return event.getTopic();
    }

    /**
     * 构建路由键
     * 格式: {domain}.{event}.{version}
     */
    private String buildRoutingKey(DomainEvent event) {
        return event.getRoutingKey();
    }
}
