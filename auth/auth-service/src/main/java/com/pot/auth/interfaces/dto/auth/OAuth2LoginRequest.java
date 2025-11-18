package com.pot.auth.interfaces.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * OAuth2登录请求
 *
 * <p>支持Google、GitHub、Facebook、Apple等OAuth2提供商
 *
 * @author yecao
 * @since 2025-11-18
 */
public record OAuth2LoginRequest(
        @NotBlank(message = "登录类型不能为空")
        String loginType,

        @NotBlank(message = "OAuth2提供商不能为空")
        String provider,  // google, github, facebook, apple

        @NotBlank(message = "授权码不能为空")
        String code,

        String state,

        @Size(max = 20, message = "用户域长度不能超过20个字符")
        String userDomain
) implements LoginRequest {
}

