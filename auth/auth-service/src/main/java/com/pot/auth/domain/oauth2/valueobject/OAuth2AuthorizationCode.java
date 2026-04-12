package com.pot.auth.domain.oauth2.valueobject;

public record OAuth2AuthorizationCode(String value) {

    public OAuth2AuthorizationCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuth2 authorization code must not be blank");
        }
    }

    public static OAuth2AuthorizationCode of(String value) {
        return new OAuth2AuthorizationCode(value);
    }
}
