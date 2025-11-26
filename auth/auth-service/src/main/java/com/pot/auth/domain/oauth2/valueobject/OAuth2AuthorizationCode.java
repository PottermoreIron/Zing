package com.pot.auth.domain.oauth2.valueobject;

/**
 * OAuth2授权码值对象
 *
 * @author yecao
 * @since 2025-11-10
 */
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
