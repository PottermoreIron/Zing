package com.pot.auth.domain.shared.valueobject;

import lombok.Builder;

@Builder
public record UserId(Long value) {

    public UserId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("UserId必须是正整数");
        }
    }

        public static UserId of(Long value) {
        return new UserId(value);
    }

        public static UserId of(String value) {
        try {
            return new UserId(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的UserId格式: " + value, e);
        }
    }
}

