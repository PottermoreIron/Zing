package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * OAuth2注册请求
 *
 * <p>支持Google、GitHub、Facebook、Apple等OAuth2提供商
 * <p>注意：OAuth2的注册和登录是一体化的，使用AuthenticationStrategy处理
 * <p>此DTO主要用于类型完整性，实际使用OAuth2LoginRequest
 *
 * @author pot
 * @since 2025-11-19
 */
public record OAuth2RegisterRequest(
        @NotNull(message = "注册类型不能为空")
        @JsonProperty("registerType")
        RegisterType registerType,

        @NotNull(message = "OAuth2提供商不能为空")
        OAuth2Provider provider,

        @NotBlank(message = "授权码不能为空")
        String code,

        String state,

        @JsonProperty("userDomain")
        UserDomain userDomain
) implements RegisterRequest {
}

