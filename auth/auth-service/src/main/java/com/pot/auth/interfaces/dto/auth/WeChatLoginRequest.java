package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 微信登录请求
 *
 * @author yecao
 * @since 2025-11-18
 */
public record WeChatLoginRequest(
        @NotNull(message = "登录类型不能为空")
        @JsonProperty("loginType")
        LoginType loginType,

        @NotBlank(message = "微信授权码不能为空")
        String code,

        String state,

        @JsonProperty("userDomain")
        UserDomain userDomain
) implements LoginRequest {
}

