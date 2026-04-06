package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One-stop authentication request for OAuth2 providers.
 */
public record OAuth2AuthRequest(
        @NotNull(message = "认证类型不能为空") @JsonProperty("authType") AuthType authType,

        @NotNull(message = "OAuth2提供商不能为空") @JsonProperty("provider") OAuth2Provider provider,

        @NotBlank(message = "授权码不能为空") @JsonProperty("code") String code,

        @JsonProperty("state") String state,

        @NotNull(message = "用户域不能为空") @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {

    public OAuth2AuthRequest {
        if (authType != null && authType != AuthType.OAUTH2) {
            throw new IllegalArgumentException("OAuth2AuthRequest 的 authType 必须是 OAUTH2");
        }
    }

    /**
     * Supported OAuth2 providers.
     */
    public enum OAuth2Provider {
        GOOGLE("google", "Google"),
        GITHUB("github", "GitHub"),
        FACEBOOK("facebook", "Facebook"),
        APPLE("apple", "Apple"),
        MICROSOFT("microsoft", "Microsoft");

        private final String code;
        private final String displayName;

        OAuth2Provider(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        /**
         * Resolves a provider from its external code.
         */
        public static OAuth2Provider fromCode(String code) {
            for (OAuth2Provider provider : values()) {
                if (provider.code.equalsIgnoreCase(code)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("未知的 OAuth2 提供商: " + code);
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
