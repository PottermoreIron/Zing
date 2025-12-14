package com.pot.member.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.member.service.entity.MemberRole;
import com.pot.member.service.entity.Permission;
import com.pot.member.service.entity.Role;
import com.pot.member.service.entity.RolePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限查询内部服务
 *
 * <p>
 * 用于auth-service查询用户权限和角色
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionQueryInternalService {

    private final MemberRoleService memberRoleService;
    private final RolePermissionService rolePermissionService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    /**
     * 查询用户权限
     *
     * @param userId 用户ID
     * @return 用户权限集合
     */
    public Set<String> queryUserPermissions(String userId) {
        log.debug("[权限查询] 查询用户权限: userId={}", userId);

        try {
            // 1. 查询用户的有效角色
            List<MemberRole> memberRoles = memberRoleService.list(
                    new LambdaQueryWrapper<MemberRole>()
                            .eq(MemberRole::getMemberId, Long.parseLong(userId))
                            .eq(MemberRole::getIsActive, 1)
                            .and(wrapper -> wrapper
                                    .isNull(MemberRole::getGmtExpiresAt)
                                    .or()
                                    .gt(MemberRole::getGmtExpiresAt, LocalDateTime.now()))
                            .isNull(MemberRole::getGmtDeletedAt));

            if (memberRoles.isEmpty()) {
                log.debug("[权限查询] 用户无角色: userId={}", userId);
                return Set.of();
            }

            // 2. 提取角色ID列表
            List<Long> roleIds = memberRoles.stream()
                    .map(MemberRole::getRoleId)
                    .distinct()
                    .collect(Collectors.toList());

            // 3. 查询角色关联的权限ID
            List<RolePermission> rolePermissions = rolePermissionService.list(
                    new LambdaQueryWrapper<RolePermission>()
                            .in(RolePermission::getRoleId, roleIds)
                            .isNull(RolePermission::getGmtDeletedAt));

            if (rolePermissions.isEmpty()) {
                log.debug("[权限查询] 角色无权限: userId={}, roleIds={}", userId, roleIds);
                return Set.of();
            }

            // 4. 提取权限ID列表
            List<Long> permissionIds = rolePermissions.stream()
                    .map(RolePermission::getPermissionId)
                    .distinct()
                    .collect(Collectors.toList());

            // 5. 查询权限详情
            List<Permission> permissions = permissionService.list(
                    new LambdaQueryWrapper<Permission>()
                            .in(Permission::getId, permissionIds)
                            .eq(Permission::getIsActive, 1)
                            .isNull(Permission::getGmtDeletedAt));

            // 6. 提取权限编码
            Set<String> permissionCodes = permissions.stream()
                    .map(Permission::getPermissionCode)
                    .collect(Collectors.toSet());

            log.info("[权限查询] 查询成功: userId={}, permissionCount={}", userId, permissionCodes.size());
            return permissionCodes;

        } catch (Exception e) {
            log.error("[权限查询] 查询用户权限失败: userId={}, error={}", userId, e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * 查询用户角色
     *
     * @param userId 用户ID
     * @return 用户角色集合
     */
    public Set<String> queryUserRoles(String userId) {
        log.debug("[权限查询] 查询用户角色: userId={}", userId);

        try {
            // 1. 查询用户的有效角色
            List<MemberRole> memberRoles = memberRoleService.list(
                    new LambdaQueryWrapper<MemberRole>()
                            .eq(MemberRole::getMemberId, Long.parseLong(userId))
                            .eq(MemberRole::getIsActive, 1)
                            .and(wrapper -> wrapper
                                    .isNull(MemberRole::getGmtExpiresAt)
                                    .or()
                                    .gt(MemberRole::getGmtExpiresAt, LocalDateTime.now()))
                            .isNull(MemberRole::getGmtDeletedAt));

            if (memberRoles.isEmpty()) {
                return Set.of();
            }

            // 2. 提取角色ID列表
            List<Long> roleIds = memberRoles.stream()
                    .map(MemberRole::getRoleId)
                    .distinct()
                    .collect(Collectors.toList());

            // 3. 查询角色详情
            List<Role> roles = roleService.list(
                    new LambdaQueryWrapper<Role>()
                            .in(Role::getId, roleIds)
                            .eq(Role::getIsActive, 1)
                            .isNull(Role::getGmtDeletedAt));

            // 4. 提取角色编码
            Set<String> roleCodes = roles.stream()
                    .map(Role::getRoleCode)
                    .collect(Collectors.toSet());

            log.info("[权限查询] 查询角色成功: userId={}, roleCount={}", userId, roleCodes.size());
            return roleCodes;

        } catch (Exception e) {
            log.error("[权限查询] 查询用户角色失败: userId={}, error={}", userId, e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * 批量查询用户权限
     *
     * @param userIds 用户ID集合
     * @return 用户ID -> 权限集合的映射
     */
    public Map<String, Set<String>> batchQueryUserPermissions(Set<String> userIds) {
        log.debug("[权限查询] 批量查询用户权限: count={}", userIds.size());
        return userIds.stream()
                .collect(Collectors.toMap(
                        userId -> userId,
                        this::queryUserPermissions,
                        (a, b) -> a,
                        HashMap::new));
    }
}
