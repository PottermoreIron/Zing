package com.pot.member.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 权限变更事件发布器
 *
 * <p>
 * 在member-service中发布权限变更事件到消息队列
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChangedPublisher {

    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;
    // TODO: 注入RabbitTemplate

    /**
     * 发布用户角色变更事件
     *
     * @param userId    用户ID
     * @param namespace 命名空间
     * @param reason    变更原因
     */
    public void publishRoleChanged(String userId, String namespace, String reason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("namespace", namespace);
            event.put("changeType", "ROLE_ASSIGNED");
            event.put("occurredAt", Instant.now().toString());
            event.put("reason", reason);

            // String message = objectMapper.writeValueAsString(event);
            // TODO: rabbitTemplate.convertAndSend("auth.permission.changed", message);

            log.info("[事件发布] 发布权限变更事件: userId={}, namespace={}, reason={}",
                    userId, namespace, reason);

        } catch (Exception e) {
            log.error("[事件发布] 发布权限变更事件失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发布角色权限变更事件
     *
     * @param roleId    角色ID
     * @param namespace 命名空间
     */
    public void publishRolePermissionChanged(String roleId, String namespace) {
        // TODO: 查询该角色下的所有用户，逐个发布事件
        log.info("[事件发布] 发布角色权限变更事件: roleId={}, namespace={}", roleId, namespace);
    }
}
