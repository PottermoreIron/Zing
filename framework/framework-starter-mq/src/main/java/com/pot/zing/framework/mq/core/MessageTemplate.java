package com.pot.zing.framework.mq.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * High-level facade for publishing messages and domain events.
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Slf4j
@RequiredArgsConstructor
public class MessageTemplate {

    private final MessageProducer messageProducer;

    /**
     * Publishes a domain event.
     */
    public void publishDomainEvent(DomainEvent event) {
        String topic = buildTopicName(event);
        String routingKey = buildRoutingKey(event);

        log.info("[MQ] Publishing domain event — topic={}, routingKey={}, eventType={}",
                topic, routingKey, event.getClass().getSimpleName());

        messageProducer.send(topic, routingKey, event);
    }

    /**
     * Publishes a domain event with publisher confirmation.
     */
    public void publishDomainEventWithConfirm(DomainEvent event, PublishCallback callback) {
        String topic = buildTopicName(event);

        log.info("[MQ] Publishing domain event with confirm — topic={}, eventType={}",
                topic, event.getClass().getSimpleName());

        messageProducer.sendWithConfirm(topic, event, callback);
    }

    /**
     * Sends a message without an explicit routing key.
     */
    public void send(String topic, Object message) {
        log.debug("[MQ] Sending message — topic={}, messageType={}", topic, message.getClass().getSimpleName());
        messageProducer.send(topic, message);
    }

    /**
     * Sends a message with an explicit routing key.
     */
    public void send(String topic, String routingKey, Object message) {
        log.debug("[MQ] Sending message — topic={}, routingKey={}, messageType={}",
                topic, routingKey, message.getClass().getSimpleName());
        messageProducer.send(topic, routingKey, message);
    }

    private String buildTopicName(DomainEvent event) {
        return event.getTopic();
    }

    private String buildRoutingKey(DomainEvent event) {
        return event.getRoutingKey();
    }
}
