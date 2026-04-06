package com.pot.member.service.domain.model.permission;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PermissionAggregate {

    private PermissionId permissionId;
    private String permissionCode;
    private String permissionName;
    private String resource;
    private String action;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

        public static PermissionAggregate create(
            String permissionCode,
            String permissionName,
            String resource,
            String action,
            String description) {
        PermissionAggregate permission = new PermissionAggregate();
        permission.permissionCode = permissionCode;
        permission.permissionName = permissionName;
        permission.resource = resource;
        permission.action = action;
        permission.description = description;
        permission.createdAt = LocalDateTime.now();
        permission.updatedAt = LocalDateTime.now();
        return permission;
    }

        public static PermissionAggregate reconstitute(
            PermissionId permissionId,
            String permissionCode,
            String permissionName,
            String resource,
            String action,
            String description,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        PermissionAggregate permission = new PermissionAggregate();
        permission.permissionId = permissionId;
        permission.permissionCode = permissionCode;
        permission.permissionName = permissionName;
        permission.resource = resource;
        permission.action = action;
        permission.description = description;
        permission.createdAt = createdAt;
        permission.updatedAt = updatedAt;
        return permission;
    }

        public void update(
            String permissionName,
            String resource,
            String action,
            String description) {
        this.permissionName = permissionName;
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
}
