package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import com.pot.auth.domain.validation.annotations.ValidUsername;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * 用户名密码注册请求
 *
 * @author pot
 * @since 2025-11-18
 */
public record UsernamePasswordRegisterRequest(
        @NotNull(message = "注册类型不能为空") @JsonProperty("registerType") RegisterType registerType,

        @ValidUsername String username,

        @ValidPassword String password,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements RegisterRequest {
}
