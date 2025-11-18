package com.pot.auth.interfaces.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 微信登录请求
 *
 * @author yecao
 * @since 2025-11-18
 */
public record WeChatLoginRequest(
        @NotBlank(message = "登录类型不能为空")
        String loginType,

        @NotBlank(message = "微信授权码不能为空")
        String code,

        String state,

        @Size(max = 20, message = "用户域长度不能超过20个字符")
        String userDomain
) implements LoginRequest {
}

