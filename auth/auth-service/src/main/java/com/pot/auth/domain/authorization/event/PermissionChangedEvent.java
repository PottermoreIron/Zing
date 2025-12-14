package com.pot.auth.domain.authorization.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 权限变更事件
 *
 * <p>
 * 当用户权限发生变更时发布此事件，用于触发权限缓存刷新
 *
 * @author pot
 * @since 2025-12-14
 */
@Data
@Builder
public class PermissionChangedEvent {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 命名空间（member, admin等）
     */
    private String namespace;

    /**
     * 变更类型
     */
    private ChangeType changeType;

    /**
     * 变更时间
     */
    private Instant occurredAt;

    /**
     * 变更原因
     */
    private String reason;

    /**
     * 变更类型
     */
    public enum ChangeType {
        /**
         * 角色分配
         */
        ROLE_ASSIGNED,

        /**
         * 角色移除
         */
        ROLE_REMOVED,

        /**
         * 权限直接变更
         */
        PERMISSION_CHANGED,

        /**
         * 角色权限变更
         */
        ROLE_PERMISSION_CHANGED
    }
}
