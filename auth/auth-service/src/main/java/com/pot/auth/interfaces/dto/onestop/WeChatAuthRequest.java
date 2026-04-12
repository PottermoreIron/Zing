package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One-stop authentication request for WeChat sign-in.
 */
public record WeChatAuthRequest(
        @NotNull(message = "Auth type must not be null") @JsonProperty("authType") AuthType authType,

        @NotBlank(message = "WeChat authorization code must not be blank") @JsonProperty("code") String code,

        @JsonProperty("state") String state,

        @NotNull(message = "User domain must not be null") @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {

    public WeChatAuthRequest {
        if (authType != null && authType != AuthType.WECHAT) {
            throw new IllegalArgumentException("WeChatAuthRequest.authType must be WECHAT");
        }
    }
}
