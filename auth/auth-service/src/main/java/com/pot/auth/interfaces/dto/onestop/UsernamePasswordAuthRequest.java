package com.pot.auth.interfaces.dto.onestop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import com.pot.auth.domain.validation.annotations.ValidUsername;
import com.pot.auth.interfaces.dto.deserializer.UserDomainDeserializer;
import jakarta.validation.constraints.NotNull;

/**
 * 用户名密码认证请求
 *
 * @author pot
 * @since 2025-11-29
 */
public record UsernamePasswordAuthRequest(
        @NotNull(message = "认证类型不能为空") @JsonProperty("authType") AuthType authType,

        @ValidUsername String username,

        @ValidPassword String password,

        @JsonProperty("userDomain") @JsonDeserialize(using = UserDomainDeserializer.class) UserDomain userDomain)
        implements OneStopAuthRequest {
}
