package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.auth.domain.validation.annotations.ValidVerificationCode;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * 邮箱验证码注册请求
 *
 * @author pot
 * @since 2025-11-18
 */
public record EmailCodeRegisterRequest(
        @NotNull(message = "注册类型不能为空") @JsonProperty("registerType") RegisterType registerType,

        @ValidEmail String email,

        @ValidVerificationCode String verificationCode,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements RegisterRequest {
}
