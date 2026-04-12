package com.pot.auth.infrastructure.listener;

import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.infrastructure.event.PermissionChangedEvent;
import com.pot.zing.framework.mq.core.MessageConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Invalidates auth permission caches when member permissions change.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChangedEventListener implements MessageConsumer<PermissionChangedEvent> {

    private final PermissionDomainService permissionDomainService;

    @Override
    public void consume(PermissionChangedEvent event) {
        log.info("[PermissionEvent] Permission change event received — changeType={}, affectedMembers={}, roleId={}, permissionId={}",
                event.getChangeType(), event.getAffectedMemberIds().size(),
                event.getRoleId(), event.getPermissionId());

        try {
            for (Long memberId : event.getAffectedMemberIds()) {
                permissionDomainService.invalidatePermissionCache(
                        new UserId(memberId),
                        UserDomain.MEMBER);
            }

            log.info("[PermissionEvent] Permission cache cleared for {} member(s)", event.getAffectedMemberIds().size());
        } catch (Exception e) {
            log.error("[PermissionEvent] Failed to process permission change event — eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            // Cache invalidation is best-effort and should not trigger message retries.
        }
    }

    @Override
    public Class<PermissionChangedEvent> getMessageType() {
        return PermissionChangedEvent.class;
    }

    @Override
    public String getQueue() {
        return "member.permission";
    }
}
