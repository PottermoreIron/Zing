package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Register request used by legacy OAuth2 flows.
 */
public record OAuth2RegisterRequest(
        @NotNull(message = "Register type must not be null") @JsonProperty("registerType") RegisterType registerType,

        @NotNull(message = "OAuth2 provider must not be null") OAuth2Provider provider,

        @NotBlank(message = "Authorization code must not be blank") String code,

        String state,

        @JsonProperty("userDomain") UserDomain userDomain) implements RegisterRequest {
}
