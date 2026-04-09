package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.validation.annotations.ValidEmail;
import com.pot.auth.interfaces.validation.annotations.ValidPassword;
import com.pot.auth.interfaces.validation.annotations.ValidVerificationCode;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * Register request for email and password.
 */
public record EmailPasswordRegisterRequest(
                @NotNull(message = "注册类型不能为空") @JsonProperty("registerType") RegisterType registerType,

                @ValidEmail String email,

                @ValidPassword String password,

                @ValidVerificationCode String verificationCode,

                @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
                implements RegisterRequest {
}
