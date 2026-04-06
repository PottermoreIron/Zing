package com.pot.auth.domain.oauth2.valueobject;

public record OAuth2AuthorizationCode(String value) {

    public OAuth2AuthorizationCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuth2授权码不能为空");
        }
    }

    public static OAuth2AuthorizationCode of(String value) {
        return new OAuth2AuthorizationCode(value);
    }
}
