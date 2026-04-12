package com.pot.member.service.domain.model.permission;

public record PermissionId(Long value) {

    public PermissionId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Permission ID must not be null and must be a positive number");
        }
    }

    public static PermissionId of(Long value) {
        return new PermissionId(value);
    }
}
