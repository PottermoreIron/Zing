package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import com.pot.auth.interfaces.validation.annotations.ValidPassword;
import com.pot.auth.interfaces.validation.annotations.ValidPhone;
import com.pot.auth.interfaces.validation.annotations.ValidVerificationCode;
import jakarta.validation.constraints.NotNull;

/**
 * Register request for phone and password credentials.
 * A verification code is required to confirm phone ownership.
 */
public record PhonePasswordRegisterRequest(
                @NotNull(message = "Register type must not be null") @JsonProperty("registerType") RegisterType registerType,

                @ValidPhone String phone,

                @ValidPassword String password,

                @ValidVerificationCode String verificationCode,

                @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
                implements RegisterRequest {
}
