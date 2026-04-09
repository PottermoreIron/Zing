package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.validation.annotations.ValidPhone;
import com.pot.auth.interfaces.validation.annotations.ValidVerificationCode;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * One-stop authentication request for phone and verification code.
 */
public record PhoneCodeAuthRequest(
                @NotNull(message = "认证类型不能为空") @JsonProperty("authType") AuthType authType,

                @ValidPhone String phone,

                @ValidVerificationCode String verificationCode,

                @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
                implements OneStopAuthRequest {
}
