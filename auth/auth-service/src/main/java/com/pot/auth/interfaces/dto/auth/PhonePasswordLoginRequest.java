package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import com.pot.auth.interfaces.validation.annotations.ValidPassword;
import com.pot.auth.interfaces.validation.annotations.ValidPhone;
import com.pot.auth.interfaces.validation.annotations.ValidVerificationCode;
import jakarta.validation.constraints.NotNull;

/**
 * Login request for phone and password credentials.
 */
public record PhonePasswordLoginRequest(
        @NotNull(message = "Login type must not be null") @JsonProperty("loginType") LoginType loginType,

        @ValidPhone String phone,

        @ValidPassword String password,

        @ValidVerificationCode String verificationCode,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements LoginRequest {
}
