package com.pot.zing.framework.mq.core;

/**
 * Transport-agnostic message consumer.
 *
 * @param <T> consumed message type
 * @author Copilot
 * @since 2026-01-05
 */
public interface MessageConsumer<T> {

    /**
     * Consumes a message payload.
     */
    void consume(T message);

    /**
     * Returns the message payload type.
     */
    Class<T> getMessageType();

    /**
     * Returns the bound queue name.
     */
    String getQueue();
}
