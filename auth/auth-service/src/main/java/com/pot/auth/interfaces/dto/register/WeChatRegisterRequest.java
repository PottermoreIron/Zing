package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Register request used by legacy WeChat flows.
 */
public record WeChatRegisterRequest(
        @NotNull(message = "Register type must not be null") @JsonProperty("registerType") RegisterType registerType,

        @NotBlank(message = "WeChat authorization code must not be blank") String code,

        String state,

        @JsonProperty("userDomain") UserDomain userDomain) implements RegisterRequest {
}
