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
        log.info("[权限变更监听] 收到权限变更事件: changeType={}, affectedMembers={}, roleId={}, permissionId={}",
                event.getChangeType(), event.getAffectedMemberIds().size(),
                event.getRoleId(), event.getPermissionId());

        try {
            for (Long memberId : event.getAffectedMemberIds()) {
                permissionDomainService.invalidatePermissionCache(
                        new UserId(memberId),
                        UserDomain.MEMBER);
            }

            log.info("[权限变更监听] 成功清除{}个会员的权限缓存", event.getAffectedMemberIds().size());
        } catch (Exception e) {
            log.error("[权限变更监听] 处理权限变更事件失败: eventId={}, error={}",
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
