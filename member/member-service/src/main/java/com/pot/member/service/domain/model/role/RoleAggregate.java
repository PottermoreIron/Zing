package com.pot.member.service.domain.model.role;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
public class RoleAggregate {

    private RoleId roleId;
    private RoleName roleName;
    private String roleCode;
    private String description;
    private Set<Long> permissionIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

        public void update(RoleName roleName, String description) {
        if (roleName != null) {
            this.roleName = roleName;
        }
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

        public void addPermission(Long permissionId) {
        if (permissionId == null || permissionId <= 0) {
            throw new IllegalArgumentException("Invalid permission ID");
        }
        this.permissionIds.add(permissionId);
        this.updatedAt = LocalDateTime.now();
    }

        public void removePermission(Long permissionId) {
        this.permissionIds.remove(permissionId);
        this.updatedAt = LocalDateTime.now();
    }

        public void setPermissions(Set<Long> permissionIds) {
        this.permissionIds = new HashSet<>(permissionIds);
        this.updatedAt = LocalDateTime.now();
    }

        public boolean hasPermission(Long permissionId) {
        return permissionIds.contains(permissionId);
    }
}
