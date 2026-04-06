package com.pot.zing.framework.mq.core;

import java.time.LocalDateTime;

/**
 * Contract for published domain events.
 *
 * @author Copilot
 * @since 2026-01-05
 */
public interface DomainEvent {

    /**
     * Returns the event identifier.
     */
    String getEventId();

    /**
     * Returns the event type.
     */
    String getEventType();

    /**
     * Returns the publish topic.
     */
    String getTopic();

    /**
     * Returns the routing key.
     */
    String getRoutingKey();

    /**
     * Returns the event timestamp.
     */
    LocalDateTime getOccurredAt();

    /**
     * Returns the aggregate identifier.
     */
    String getAggregateId();
}
