package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * OAuth2认证请求
 *
 * @author pot
 * @since 2025-11-29
 */
public record OAuth2AuthRequest(
        @NotNull(message = "认证类型不能为空") @JsonProperty("authType") AuthType authType,

        @NotBlank(message = "OAuth2提供商不能为空") String provider,

        @NotBlank(message = "授权码不能为空") String code,

        String state,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {
}
