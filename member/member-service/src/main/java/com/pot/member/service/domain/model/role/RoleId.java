package com.pot.member.service.domain.model.role;

public record RoleId(Long value) {

    public RoleId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Role ID must not be null and must be a positive number");
        }
    }

    public static RoleId of(Long value) {
        return new RoleId(value);
    }
}
