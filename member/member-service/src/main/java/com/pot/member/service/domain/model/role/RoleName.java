package com.pot.member.service.domain.model.role;

import lombok.Value;

@Value
public class RoleName {
    String value;

    public RoleName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("角色名称不能超过50个字符");
        }
        this.value = trimmed;
    }

    public static RoleName of(String value) {
        return new RoleName(value);
    }
}
