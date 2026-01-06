package com.pot.member.service.domain.model.role;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 角色聚合根
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Getter
public class RoleAggregate {

    private RoleId roleId;
    private RoleName roleName;
    private String roleCode;
    private String description;
    private Set<Long> permissionIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 创建新角色
     */
    public static RoleAggregate create(
            RoleName roleName,
            String roleCode,
            String description) {
        RoleAggregate role = new RoleAggregate();
        role.roleName = roleName;
        role.roleCode = roleCode;
        role.description = description;
        role.permissionIds = new HashSet<>();
        role.createdAt = LocalDateTime.now();
        role.updatedAt = LocalDateTime.now();
        return role;
    }

    /**
     * 重建角色（从数据库加载）
     */
    public static RoleAggregate reconstitute(
            RoleId roleId,
            RoleName roleName,
            String roleCode,
            String description,
            Set<Long> permissionIds,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        RoleAggregate role = new RoleAggregate();
        role.roleId = roleId;
        role.roleName = roleName;
        role.roleCode = roleCode;
        role.description = description;
        role.permissionIds = permissionIds != null ? new HashSet<>(permissionIds) : new HashSet<>();
        role.createdAt = createdAt;
        role.updatedAt = updatedAt;
        return role;
    }

    /**
     * 更新角色信息
     */
    public void update(RoleName roleName, String description) {
        if (roleName != null) {
            this.roleName = roleName;
        }
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加权限
     */
    public void addPermission(Long permissionId) {
        if (permissionId == null || permissionId <= 0) {
            throw new IllegalArgumentException("权限ID无效");
        }
        this.permissionIds.add(permissionId);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 移除权限
     */
    public void removePermission(Long permissionId) {
        this.permissionIds.remove(permissionId);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 批量设置权限
     */
    public void setPermissions(Set<Long> permissionIds) {
        this.permissionIds = new HashSet<>(permissionIds);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否拥有某个权限
     */
    public boolean hasPermission(Long permissionId) {
        return permissionIds.contains(permissionId);
    }
}
