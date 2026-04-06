package com.pot.member.service.domain.model.permission;

public record PermissionId(Long value) {

    public PermissionId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("权限ID不能为空且必须为正数");
        }
    }

    public static PermissionId of(Long value) {
        return new PermissionId(value);
    }
}
