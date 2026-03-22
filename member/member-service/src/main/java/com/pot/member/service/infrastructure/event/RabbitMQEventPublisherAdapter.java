package com.pot.member.service.infrastructure.event;

import com.pot.member.service.domain.event.MemberDomainEvent;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.zing.framework.mq.core.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 领域事件发布适配器
 *
 * <p>
 * 实现领域层的 {@link DomainEventPublisher} 端口，
 * 通过 framework-starter-mq 的 {@link MessageProducer} 发布到 RabbitMQ。
 *
 * @author Pot
 * @since 2026-03-18
 */
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
            throw new RuntimeException("领域事件发布失败: " + event.getEventType(), e);
        }
    }
}
