package com.pot.member.service.domain.model.role;

public record RoleId(Long value) {

    public RoleId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("角色ID不能为空且必须为正数");
        }
    }

    public static RoleId of(Long value) {
        return new RoleId(value);
    }
}
