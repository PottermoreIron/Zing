package com.pot.member.service.domain.event;

import lombok.Getter;

import java.util.Set;

/**
 * 权限变更领域事件
 *
 * <p>
 * 当用户权限发生变更时发布，用于通知 auth-service 刷新权限缓存。
 * 继承 {@link MemberDomainEvent}，统一路由至 {@code member.events} Exchange。
 *
 * @author Pot
 * @since 2026-03-18
 */
@Getter
public class PermissionChangedEvent extends MemberDomainEvent {

    /** 受影响的会员ID集合 */
    private final Set<Long> affectedMemberIds;

    /** 变更类型 */
    private final ChangeType changeType;

    /** 变更的角色ID（角色变更时填充） */
    private final Long roleId;

    /** 变更的权限ID（权限变更时填充） */
    private final Long permissionId;

    /** 变更原因/操作人 */
    private final String reason;

    public PermissionChangedEvent(String aggregateId,
            Set<Long> affectedMemberIds,
            ChangeType changeType,
            Long roleId,
            Long permissionId,
            String reason) {
        super(aggregateId);
        this.affectedMemberIds = affectedMemberIds;
        this.changeType = changeType;
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.reason = reason;
    }

    @Override
    protected String getEventName() {
        return "permission.changed";
    }

    /**
     * 权限变更类型
     */
    public enum ChangeType {
        /**
         * 会员角色分配/撤销
         */
        MEMBER_ROLE_ASSIGNED,
        MEMBER_ROLE_REVOKED,

        /**
         * 角色权限变更
         */
        ROLE_PERMISSION_ADDED,
        ROLE_PERMISSION_REMOVED,

        /**
         * 权限本身更新
         */
        PERMISSION_UPDATED,

        /**
         * 角色更新
         */
        ROLE_UPDATED
    }
}
