package com.pot.auth.domain.shared.valueobject;

import java.util.UUID;

public record TokenId(String value) {

    public TokenId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TokenId must not be blank");
        }
    }

        public static TokenId generate() {
        return new TokenId(UUID.randomUUID().toString());
    }

    public static TokenId of(String value) {
        return new TokenId(value);
    }
}

