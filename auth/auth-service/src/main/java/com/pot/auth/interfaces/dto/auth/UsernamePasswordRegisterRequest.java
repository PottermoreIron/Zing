package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import com.pot.auth.domain.validation.annotations.ValidUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 用户名密码注册请求
 *
 * @author yecao
 * @since 2025-11-18
 */
public record UsernamePasswordRegisterRequest(
        @NotNull(message = "注册类型不能为空")
        @JsonProperty("registerType")
        RegisterType registerType,

        @NotBlank(message = "用户名不能为空")
        @ValidUsername(message = "用户名只允许中文、字母、数字、下划线、连字符，长度在1-30个字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @ValidPassword(message = "密码必须包含大小写字母、数字、特殊字符，且长度在8-16个字符之间")
        String password,

        @JsonProperty("userDomain")
        UserDomain userDomain
) implements RegisterRequest {
}

