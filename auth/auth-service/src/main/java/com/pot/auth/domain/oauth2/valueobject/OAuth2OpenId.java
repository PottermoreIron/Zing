package com.pot.auth.domain.oauth2.valueobject;

public record OAuth2OpenId(String value) {

    public OAuth2OpenId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuth2 OpenID不能为空");
        }
    }
}

