package com.pot.zing.framework.mq.core;

/**
 * Transport-agnostic message producer.
 *
 * @author Copilot
 * @since 2026-01-05
 */
public interface MessageProducer {

    /**
     * Sends a message to a topic or queue.
     */
    void send(String topic, Object message);

    /**
     * Sends a message with an explicit routing or partition key.
     */
    void send(String topic, String routingKey, Object message);

    /**
     * Sends a message and reports the publish result through a callback.
     */
    void sendWithConfirm(String topic, Object message, PublishCallback callback);
}
