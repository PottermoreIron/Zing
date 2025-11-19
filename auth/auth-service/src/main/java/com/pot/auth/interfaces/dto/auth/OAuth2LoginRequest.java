package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * OAuth2登录请求
 *
 * <p>支持Google、GitHub、Facebook、Apple等OAuth2提供商
 *
 * @author yecao
 * @since 2025-11-18
 */
public record OAuth2LoginRequest(
        @NotNull(message = "登录类型不能为空")
        @JsonProperty("loginType")
        LoginType loginType,

        @NotNull(message = "OAuth2提供商不能为空")
        OAuth2Provider provider,

        @NotBlank(message = "授权码不能为空")
        String code,

        String state,

        @JsonProperty("userDomain")
        UserDomain userDomain
) implements LoginRequest {
}

