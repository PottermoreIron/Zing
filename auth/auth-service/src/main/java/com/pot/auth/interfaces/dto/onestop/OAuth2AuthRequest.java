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
        @NotNull(message = "Auth type must not be null") @JsonProperty("authType") AuthType authType,

        @NotNull(message = "OAuth2 provider must not be null") @JsonProperty("provider") OAuth2Provider provider,

        @NotBlank(message = "Authorization code must not be blank") @JsonProperty("code") String code,

        @JsonProperty("state") String state,

        @JsonProperty("redirectUri") String redirectUri,

        @NotNull(message = "User domain must not be null") @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {

    public OAuth2AuthRequest {
        if (authType != null && authType != AuthType.OAUTH2) {
            throw new IllegalArgumentException("OAuth2AuthRequest.authType must be OAUTH2");
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
            throw new IllegalArgumentException("Unknown OAuth2 provider: " + code);
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
