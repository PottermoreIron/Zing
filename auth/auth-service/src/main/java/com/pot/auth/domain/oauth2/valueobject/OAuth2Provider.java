package com.pot.auth.domain.oauth2.valueobject;

import lombok.Getter;

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

        public static OAuth2Provider fromCode(String code) {
        for (OAuth2Provider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unsupported OAuth2 provider: " + code);
    }
}