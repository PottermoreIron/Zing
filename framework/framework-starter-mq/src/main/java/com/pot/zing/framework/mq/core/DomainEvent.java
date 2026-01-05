package com.pot.zing.framework.mq.core;

import java.time.LocalDateTime;

/**
 * 领域事件基类
 *
 * @author Copilot
 * @since 2026-01-05
 */
public interface DomainEvent {

    /**
     * 获取事件ID
     */
    String getEventId();

    /**
     * 获取事件类型
     */
    String getEventType();

    /**
     * 获取Topic名称
     * 格式: {domain}.{event}.{version}
     * 例如: member.permission.changed.v1
     */
    String getTopic();

    /**
     * 获取路由键
     * 格式: {domain}.{event}.{version}
     */
    String getRoutingKey();

    /**
     * 获取事件发生时间
     */
    LocalDateTime getOccurredAt();

    /**
     * 获取聚合根ID
     */
    String getAggregateId();
}
