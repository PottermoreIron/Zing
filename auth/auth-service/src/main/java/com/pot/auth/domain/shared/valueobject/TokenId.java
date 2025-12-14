package com.pot.auth.domain.shared.valueobject;

import java.util.UUID;

/**
 * Token ID值对象
 *
 * @author pot
 * @since 2025-12-14
 */
public record TokenId(String value) {

    public TokenId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TokenId不能为空");
        }
    }

    /**
     * 生成新的TokenId (JTI - JWT ID)
     */
    public static TokenId generate() {
        return new TokenId(UUID.randomUUID().toString());
    }

    public static TokenId of(String value) {
        return new TokenId(value);
    }
}

