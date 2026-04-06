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
        @NotNull(message = "注册类型不能为空") @JsonProperty("registerType") RegisterType registerType,

        @NotNull(message = "OAuth2提供商不能为空") OAuth2Provider provider,

        @NotBlank(message = "授权码不能为空") String code,

        String state,

        @JsonProperty("userDomain") UserDomain userDomain) implements RegisterRequest {
}
