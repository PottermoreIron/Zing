package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * 邮箱密码登录请求
 *
 * @author pot
 * @since 2025-11-18
 */
public record EmailPasswordLoginRequest(
        @NotNull(message = "登录类型不能为空") @JsonProperty("loginType") LoginType loginType,

        @ValidEmail String email,

        @ValidPassword String password,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements LoginRequest {
}
