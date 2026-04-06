package com.pot.member.service.domain.event;

import com.pot.zing.framework.mq.core.AbstractDomainEvent;

/**
 * Base class for member domain events.
 *
 * <p>
 * All events emitted by member-service extend this type and are routed through
 * the
 * {@code member.events} exchange.
 * </p>
 *
 * @author Pot
 * @since 2026-03-18
 */
public abstract class MemberDomainEvent extends AbstractDomainEvent {

    protected MemberDomainEvent(String aggregateId) {
        super(aggregateId);
    }

    @Override
    protected final String getDomainName() {
        return "member";
    }
}
