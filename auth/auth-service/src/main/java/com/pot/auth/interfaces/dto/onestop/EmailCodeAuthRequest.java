package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.auth.domain.validation.annotations.ValidVerificationCode;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * 邮箱验证码认证请求
 *
 * @author pot
 * @since 2025-11-29
 */
public record EmailCodeAuthRequest(
        @NotNull(message = "认证类型不能为空") @JsonProperty("authType") AuthType authType,

        @ValidEmail String email,

        @ValidVerificationCode String verificationCode,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {
}
