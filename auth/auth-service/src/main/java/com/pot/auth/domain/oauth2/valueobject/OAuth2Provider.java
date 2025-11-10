package com.pot.auth.domain.oauth2.valueobject;

import lombok.Getter;

/**
 * OAuth2提供商枚举
 *
 * <p>定义系统支持的OAuth2第三方登录提供商
 *
 * @author yecao
 * @since 2025-11-10
 */
@Getter
public enum OAuth2Provider {

    /**
     * Google OAuth2
     */
    GOOGLE("google", "Google"),

    /**
     * GitHub OAuth2
     */
    GITHUB("github", "GitHub"),

    /**
     * Facebook OAuth2
     */
    FACEBOOK("facebook", "Facebook"),

    /**
     * Apple OAuth2
     */
    APPLE("apple", "Apple");

    private final String code;
    private final String displayName;

    OAuth2Provider(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 从code获取枚举
     */
    public static OAuth2Provider fromCode(String code) {
        for (OAuth2Provider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("不支持的OAuth2提供商: " + code);
    }
}