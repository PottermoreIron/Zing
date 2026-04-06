package com.pot.member.service.domain.event;

import lombok.Getter;

import java.util.Set;

/**
 * Domain event emitted when member permissions change.
 *
 * <p>
 * The auth-service consumes this event to refresh permission caches. Like all
 * member events,
 * it is routed through the {@code member.events} exchange.
 * </p>
 *
 * @author Pot
 * @since 2026-03-18
 */
@Getter
public class PermissionChangedEvent extends MemberDomainEvent {

    /**
     * Affected member IDs.
     */
    private final Set<Long> affectedMemberIds;

    /**
     * Change type.
     */
    private final ChangeType changeType;

    /**
     * Changed role ID, when the event is role-related.
     */
    private final Long roleId;

    /**
     * Changed permission ID, when the event is permission-related.
     */
    private final Long permissionId;

    /**
     * Change reason or operator identity.
     */
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
     * Permission change types.
     */
    public enum ChangeType {
        /** Member role assignment or revocation. */
        MEMBER_ROLE_ASSIGNED,
        MEMBER_ROLE_REVOKED,

        /** Role permission changes. */
        ROLE_PERMISSION_ADDED,
        ROLE_PERMISSION_REMOVED,

        /** Permission definition updates. */
        PERMISSION_UPDATED,

        /** Role updates. */
        ROLE_UPDATED
    }
}
