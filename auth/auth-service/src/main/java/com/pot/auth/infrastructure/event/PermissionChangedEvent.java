package com.pot.auth.infrastructure.event;

import com.pot.zing.framework.mq.core.AbstractDomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Event published when member permissions change.
 */
@Getter
@NoArgsConstructor
public class PermissionChangedEvent extends AbstractDomainEvent {

    private Set<Long> affectedMemberIds;

    private ChangeType changeType;

    private Long roleId;

    private Long permissionId;

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
     * Type of permission change.
     */
    public enum ChangeType {
        MEMBER_ROLE_ASSIGNED,
        MEMBER_ROLE_REVOKED,

        ROLE_PERMISSION_ADDED,
        ROLE_PERMISSION_REMOVED,

        PERMISSION_UPDATED,

        ROLE_UPDATED
    }
}
