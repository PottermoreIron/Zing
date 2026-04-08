package com.pot.member.service.infrastructure.event;

import com.pot.member.service.domain.event.MemberDomainEvent;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.member.service.infrastructure.exception.MemberInfrastructureException;
import com.pot.zing.framework.mq.core.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQEventPublisherAdapter implements DomainEventPublisher {

    private final MessageProducer messageProducer;

    @Override
    public void publish(MemberDomainEvent event) {
        try {
            messageProducer.send(event.getTopic(), event.getRoutingKey(), event);
            log.info("领域事件已发布: eventType={}, aggregateId={}, topic={}, routingKey={}",
                    event.getEventType(), event.getAggregateId(), event.getTopic(), event.getRoutingKey());
        } catch (Exception e) {
            log.error("领域事件发布失败: eventType={}, aggregateId={}, error={}",
                    event.getEventType(), event.getAggregateId(), e.getMessage(), e);
            throw new MemberInfrastructureException("领域事件发布失败: " + event.getEventType(), e);
        }
    }
}
