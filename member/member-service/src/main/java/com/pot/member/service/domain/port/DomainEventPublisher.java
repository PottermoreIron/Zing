package com.pot.member.service.domain.port;

import com.pot.member.service.domain.event.MemberDomainEvent;

/**
 * Outbound port for publishing domain events.
 *
 * <p>
 * The domain layer depends on this abstraction instead of a concrete MQ
 * implementation.
 * Infrastructure provides the concrete adapter.
 * </p>
 *
 * @author Pot
 * @since 2026-03-18
 */
public interface DomainEventPublisher {

    /**
     * Publish a domain event.
     */
    void publish(MemberDomainEvent event);
}
