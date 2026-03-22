package com.pot.member.service.domain.port;

import com.pot.member.service.domain.event.MemberDomainEvent;

/**
 * 领域事件发布 Port（出站端口）
 *
 * <p>
 * 领域层通过此接口发布事件，不依赖任何 MQ 实现。
 * 基础设施层提供 {@code RabbitMQEventPublisherAdapter} 实现。
 *
 * @author Pot
 * @since 2026-03-18
 */
public interface DomainEventPublisher {

    /**
     * 发布单个领域事件
     */
    void publish(MemberDomainEvent event);
}
