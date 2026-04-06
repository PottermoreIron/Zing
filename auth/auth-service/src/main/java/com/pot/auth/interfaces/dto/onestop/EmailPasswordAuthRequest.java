package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * One-stop authentication request for email and password.
 */
public record EmailPasswordAuthRequest(
                @NotNull(message = "认证类型不能为空") @JsonProperty("authType") AuthType authType,

                @ValidEmail String email,

                String password,

                String verificationCode,

                @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
                implements OneStopAuthRequest {
}
