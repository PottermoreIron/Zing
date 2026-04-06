package com.pot.zing.framework.mq.core;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base implementation for domain events.
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Data
public abstract class AbstractDomainEvent implements DomainEvent {

    /**
     * Event identifier.
     */
    private String eventId = UUID.randomUUID().toString();

    /**
     * Event timestamp.
     */
    private LocalDateTime occurredAt = LocalDateTime.now();

    /**
     * Aggregate identifier.
     */
    private String aggregateId;

    protected AbstractDomainEvent() {
    }

    protected AbstractDomainEvent(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getTopic() {
        return getDomainName() + ".events";
    }

    @Override
    public String getRoutingKey() {
        return getDomainName() + "." + getEventName() + "." + getVersion();
    }

    /**
     * Returns the domain name segment.
     */
    protected abstract String getDomainName();

    /**
     * Returns the event name segment.
     */
    protected abstract String getEventName();

    /**
     * Returns the event version segment.
     */
    protected String getVersion() {
        return "v1";
    }
}
