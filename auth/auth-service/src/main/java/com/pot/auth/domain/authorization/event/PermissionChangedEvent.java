package com.pot.auth.domain.authorization.event;

import java.time.Instant;

public record PermissionChangedEvent(
                String userId,
                String namespace,
                ChangeType changeType,
                Instant occurredAt,
                String reason) {
        public enum ChangeType {
                ROLE_ASSIGNED,
                ROLE_REMOVED,
                PERMISSION_CHANGED,
                ROLE_PERMISSION_CHANGED
        }
}
