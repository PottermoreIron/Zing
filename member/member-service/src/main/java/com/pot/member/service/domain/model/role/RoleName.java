package com.pot.member.service.domain.model.role;

import lombok.Value;

@Value
public class RoleName {
    String value;

    public RoleName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role name must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("Role name must not exceed 50 characters");
        }
        this.value = trimmed;
    }

    public static RoleName of(String value) {
        return new RoleName(value);
    }
}
