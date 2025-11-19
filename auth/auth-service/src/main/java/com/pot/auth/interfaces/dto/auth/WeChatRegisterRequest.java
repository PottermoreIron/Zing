package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
        @NotNull(message = "注册类型不能为空")
        @JsonProperty("registerType")
        RegisterType registerType,

        @NotBlank(message = "微信授权码不能为空")
        String code,

        String state,

        @JsonProperty("userDomain")
        UserDomain userDomain
) implements RegisterRequest {
}

