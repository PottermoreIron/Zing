package com.pot.auth.domain.oauth2.valueobject;

/**
 * OAuth2 OpenID值对象
 *
 * @author pot
 * @since 2025-11-10
 */
public record OAuth2OpenId(String value) {

    public OAuth2OpenId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuth2 OpenID不能为空");
        }
    }
}

