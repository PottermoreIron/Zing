package com.pot.member.service.domain.event;

import com.pot.zing.framework.mq.core.AbstractDomainEvent;
import lombok.Getter;

import java.util.Set;

/**
 * 权限变更领域事件
 * 
 * 当用户权限发生变更时发布，用于通知auth-service刷新权限缓存
 */
@Getter
public class PermissionChangedEvent extends AbstractDomainEvent {

    /**
     * 受影响的会员ID列表
     */
    private final Set<Long> affectedMemberIds;

    /**
     * 变更类型
     */
    private final ChangeType changeType;

    /**
     * 变更的角色ID（如果是角色变更）
     */
    private final Long roleId;

    /**
     * 变更的权限ID（如果是权限变更）
     */
    private final Long permissionId;

    /**
     * 变更原因/操作人
     */
    private final String reason;

    public PermissionChangedEvent(Set<Long> affectedMemberIds, ChangeType changeType,
            Long roleId, Long permissionId, String reason) {
        super();
        this.affectedMemberIds = affectedMemberIds;
        this.changeType = changeType;
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "member.permission.changed";
    }

    @Override
    protected String getDomainName() {
        return "member";
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
