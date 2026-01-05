package com.pot.zing.framework.mq.core;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件抽象基类
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Data
public abstract class AbstractDomainEvent implements DomainEvent {

    /**
     * 事件ID
     */
    private String eventId = UUID.randomUUID().toString();

    /**
     * 事件发生时间
     */
    private LocalDateTime occurredAt = LocalDateTime.now();

    /**
     * 聚合根ID
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
        // 默认实现：{domain}.events
        return getDomainName() + ".events";
    }

    @Override
    public String getRoutingKey() {
        // 默认实现：{domain}.{event}.{version}
        return getDomainName() + "." + getEventName() + "." + getVersion();
    }

    /**
     * 获取领域名称（子类实现）
     */
    protected abstract String getDomainName();

    /**
     * 获取事件名称（子类实现）
     */
    protected abstract String getEventName();

    /**
     * 获取事件版本（默认v1）
     */
    protected String getVersion() {
        return "v1";
    }
}
