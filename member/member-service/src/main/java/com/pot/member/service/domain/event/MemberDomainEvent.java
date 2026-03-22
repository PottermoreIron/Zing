package com.pot.member.service.domain.event;

import com.pot.zing.framework.mq.core.AbstractDomainEvent;

/**
 * 会员领域事件基类
 *
 * <p>
 * 所有 member-service 产生的领域事件都继承此类，统一路由到 {@code member.events} Exchange。
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
