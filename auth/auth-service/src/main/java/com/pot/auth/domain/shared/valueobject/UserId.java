package com.pot.auth.domain.shared.valueobject;

import lombok.Builder;

@Builder
public record UserId(Long value) {

    public UserId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("UserId must be a positive integer");
        }
    }

        public static UserId of(Long value) {
        return new UserId(value);
    }

        public static UserId of(String value) {
        try {
            return new UserId(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid UserId format: " + value, e);
        }
    }
}

