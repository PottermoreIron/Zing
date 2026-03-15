package com.pot.member.service.domain.event;

import com.pot.zing.framework.mq.core.MessageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 权限变更事件发布器
 */
@Slf4j
@Component
public class PermissionChangeEventPublisher {

        @Autowired(required = false)
        private MessageTemplate messageTemplate;

        private void send(String topic, PermissionChangedEvent event) {
                if (messageTemplate == null) {
                        log.warn("[PermissionChangeEventPublisher] MQ 不可用，跳过事件发布: topic={}, type={}", topic,
                                        event.getChangeType());
                        return;
                }
                messageTemplate.send(topic, event);
        }

        /**
         * 发布会员角色分配事件
         */
        public void publishMemberRoleAssigned(Long memberId, Long roleId, String operator) {
                PermissionChangedEvent event = new PermissionChangedEvent(
                                Set.of(memberId),
                                PermissionChangedEvent.ChangeType.MEMBER_ROLE_ASSIGNED,
                                roleId,
                                null,
                                "Assigned by " + operator);
                send("member.permission", event);
                log.info("Published MEMBER_ROLE_ASSIGNED event for member: {}, role: {}", memberId, roleId);
        }

        /**
         * 发布会员角色撤销事件
         */
        public void publishMemberRoleRevoked(Long memberId, Long roleId, String operator) {
                PermissionChangedEvent event = new PermissionChangedEvent(
                                Set.of(memberId),
                                PermissionChangedEvent.ChangeType.MEMBER_ROLE_REVOKED,
                                roleId,
                                null,
                                "Revoked by " + operator);
                send("member.permission", event);
                log.info("Published MEMBER_ROLE_REVOKED event for member: {}, role: {}", memberId, roleId);
        }

        /**
         * 发布角色权限添加事件
         */
        public void publishRolePermissionAdded(Set<Long> affectedMemberIds, Long roleId, Long permissionId,
                        String operator) {
                PermissionChangedEvent event = new PermissionChangedEvent(
                                affectedMemberIds,
                                PermissionChangedEvent.ChangeType.ROLE_PERMISSION_ADDED,
                                roleId,
                                permissionId,
                                "Added by " + operator);
                send("member.permission", event);
                log.info("Published ROLE_PERMISSION_ADDED event for role: {}, permission: {}, affected members: {}",
                                roleId, permissionId, affectedMemberIds.size());
        }

        /**
         * 发布角色权限移除事件
         */
        public void publishRolePermissionRemoved(Set<Long> affectedMemberIds, Long roleId, Long permissionId,
                        String operator) {
                PermissionChangedEvent event = new PermissionChangedEvent(
                                affectedMemberIds,
                                PermissionChangedEvent.ChangeType.ROLE_PERMISSION_REMOVED,
                                roleId,
                                permissionId,
                                "Removed by " + operator);
                send("member.permission", event);
                log.info("Published ROLE_PERMISSION_REMOVED event for role: {}, permission: {}, affected members: {}",
                                roleId, permissionId, affectedMemberIds.size());
        }

        /**
         * 发布角色更新事件
         */
        public void publishRoleUpdated(Set<Long> affectedMemberIds, Long roleId, String operator) {
                PermissionChangedEvent event = new PermissionChangedEvent(
                                affectedMemberIds,
                                PermissionChangedEvent.ChangeType.ROLE_UPDATED,
                                roleId,
                                null,
                                "Updated by " + operator);
                send("member.permission", event);
                log.info("Published ROLE_UPDATED event for role: {}, affected members: {}",
                                roleId, affectedMemberIds.size());
        }
}
