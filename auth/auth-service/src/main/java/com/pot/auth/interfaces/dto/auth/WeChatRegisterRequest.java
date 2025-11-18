package com.pot.auth.interfaces.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 微信注册请求
 *
 * <p>处理微信授权登录
 * <p>注意：微信的注册和登录是一体化的，使用AuthenticationStrategy处理
 * <p>此DTO主要用于类型完整性，实际使用WeChatLoginRequest
 *
 * @author yecao
 * @since 2025-11-19
 */
public record WeChatRegisterRequest(
        @NotBlank(message = "注册类型不能为空")
        String registerType,

        @NotBlank(message = "微信授权码不能为空")
        String code,

        String state,

        @Size(max = 20, message = "用户域长度不能超过20个字符")
        String userDomain
) implements RegisterRequest {
}

