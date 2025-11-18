package com.pot.auth.interfaces.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * OAuth2注册请求
 *
 * <p>支持Google、GitHub、Facebook、Apple等OAuth2提供商
 * <p>注意：OAuth2的注册和登录是一体化的，使用AuthenticationStrategy处理
 * <p>此DTO主要用于类型完整性，实际使用OAuth2LoginRequest
 *
 * @author yecao
 * @since 2025-11-19
 */
public record OAuth2RegisterRequest(
        @NotBlank(message = "注册类型不能为空")
        String registerType,

        @NotBlank(message = "OAuth2提供商不能为空")
        String provider,  // google, github, facebook, apple

        @NotBlank(message = "授权码不能为空")
        String code,

        String state,

        @Size(max = 20, message = "用户域长度不能超过20个字符")
        String userDomain
) implements RegisterRequest {
}

