package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import com.pot.auth.domain.validation.annotations.ValidUsername;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * Login request for nickname and password.
 */
public record UsernamePasswordLoginRequest(
        @NotNull(message = "登录类型不能为空") @JsonProperty("loginType") LoginType loginType,

        @ValidUsername String nickname,

        @ValidPassword String password,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements LoginRequest {
}
