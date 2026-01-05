package com.pot.auth.infrastructure.event;

import com.pot.zing.framework.mq.core.AbstractDomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 权限变更领域事件
 * 
 * 从member-service接收的权限变更事件
 */
@Getter
@NoArgsConstructor
public class PermissionChangedEvent extends AbstractDomainEvent {

    /**
     * 受影响的会员ID列表
     */
    private Set<Long> affectedMemberIds;

    /**
     * 变更类型
     */
    private ChangeType changeType;

    /**
     * 变更的角色ID（如果是角色变更）
     */
    private Long roleId;

    /**
     * 变更的权限ID（如果是权限变更）
     */
    private Long permissionId;

    /**
     * 变更原因/操作人
     */
    private String reason;

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
