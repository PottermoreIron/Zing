package com.pot.auth.domain.authorization.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.domain.authorization.event.PermissionChangedEvent;
import com.pot.auth.domain.authorization.service.PermissionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 权限变更事件监听器
 *
 * <p>
 * 监听权限变更消息队列，触发权限缓存刷新：
 * <ul>
 * <li>递增权限版本号</li>
 * <li>失效Redis缓存</li>
 * <li>使旧Token的权限验证失败</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChangedListener {

    private final PermissionDomainService permissionDomainService;
    private final ObjectMapper objectMapper;

    /**
     * 处理权限变更消息
     *
     * @param message 消息内容（JSON格式）
     */
    // @RabbitListener(queues =
    // "${auth.permission.mq.queue:auth.permission.changed}")
    public void handlePermissionChanged(String message) {
        try {
            PermissionChangedEvent event = objectMapper.readValue(message, PermissionChangedEvent.class);

            log.info("[权限变更] 收到权限变更事件: userId={}, namespace={}, type={}, reason={}",
                    event.getUserId(), event.getNamespace(), event.getChangeType(), event.getReason());

            // 1. 递增权限版本号
            permissionDomainService.incrementPermissionVersion(event.getNamespace(), event.getUserId());

            // 2. 失效权限缓存
            permissionDomainService.invalidatePermissionCache(event.getNamespace(), event.getUserId());

            log.info("[权限变更] 权限缓存已刷新: userId={}, namespace={}", event.getUserId(), event.getNamespace());

        } catch (Exception e) {
            log.error("[权限变更] 处理权限变更事件失败: message={}, error={}", message, e.getMessage(), e);
        }
    }
}
