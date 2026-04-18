package com.pot.member.service.infrastructure.event;

import com.pot.member.service.domain.event.MemberDomainEvent;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.zing.framework.mq.core.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Relays domain events to RabbitMQ after the enclosing database transaction
 * commits.
 *
 * <p>
 * Calling {@link #publish} within a {@code @Transactional} method schedules the
 * event as a Spring application event. The actual MQ dispatch happens inside
 * {@link #dispatchToMQ} which runs only after the transaction commits
 * successfully.
 * This prevents partial states where a message is sent but the DB change rolls
 * back,
 * and ensures the domain object is durably persisted before any consumer acts
 * on it.
 *
 * <p>
 * If the transaction rolls back the Spring event is discarded and no message is
 * sent. If MQ dispatch fails after commit, the event is lost; use an outbox
 * table
 * for guaranteed delivery in high-reliability scenarios.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQEventPublisherAdapter implements DomainEventPublisher {

    private final MessageProducer messageProducer;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(MemberDomainEvent event) {
        // Publish a Spring application event. When called inside a @Transactional
        // method, the @TransactionalEventListener below fires only after commit.
        applicationEventPublisher.publishEvent(new MemberDomainEventWrapper(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void dispatchToMQ(MemberDomainEventWrapper wrapper) {
        MemberDomainEvent event = wrapper.event();
        try {
            messageProducer.send(event.getTopic(), event.getRoutingKey(), event);
            log.info("[Event] Domain event dispatched — eventType={}, aggregateId={}, topic={}, routingKey={}",
                    event.getEventType(), event.getAggregateId(), event.getTopic(), event.getRoutingKey());
        } catch (Exception e) {
            // The DB transaction has already committed; log and continue rather than
            // crashing the caller. Implement an outbox pattern for guaranteed delivery.
            log.error("[Event] Failed to dispatch domain event — eventType={}, aggregateId={}, error={}",
                    event.getEventType(), event.getAggregateId(), e.getMessage(), e);
        }
    }

    /** Spring application event wrapper carrying a single domain event. */
    public record MemberDomainEventWrapper(MemberDomainEvent event) {
    }
}
