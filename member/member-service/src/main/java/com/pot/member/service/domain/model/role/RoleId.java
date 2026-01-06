package com.pot.member.service.domain.model.role;

/**
 * 角色ID值对象
 * 
 * @author Pot
 * @since 2026-01-06
 */
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
