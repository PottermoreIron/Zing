package com.pot.auth.domain.authorization.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PermissionChangedEvent {

        private String userId;

        private String namespace;

        private ChangeType changeType;

        private Instant occurredAt;

        private String reason;

        public enum ChangeType {
                ROLE_ASSIGNED,

                ROLE_REMOVED,

                PERMISSION_CHANGED,

                ROLE_PERMISSION_CHANGED
    }
}
