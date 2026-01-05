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
 * 权限变更事件监听器
 * 
 * 监听来自member-service的权限变更事件，清除auth-service中的权限缓存
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
            // 清除所有受影响会员的权限缓存
            for (Long memberId : event.getAffectedMemberIds()) {
                // 使用member域，因为事件来自member-service
                permissionDomainService.invalidatePermissionCache(
                        new UserId(memberId),
                        UserDomain.MEMBER);
            }

            log.info("[权限变更监听] 成功清除{}个会员的权限缓存", event.getAffectedMemberIds().size());
        } catch (Exception e) {
            log.error("[权限变更监听] 处理权限变更事件失败: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            // 注意：这里不抛出异常，避免消息重试造成重复处理
            // 缓存失效失败不是致命错误，下次用户请求时会重新加载
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
