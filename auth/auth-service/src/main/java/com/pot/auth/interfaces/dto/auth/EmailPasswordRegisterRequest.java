package com.pot.auth.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 邮箱密码注册请求
 *
 * @author yecao
 * @since 2025-11-18
 */
public record EmailPasswordRegisterRequest(
        @NotNull(message = "注册类型不能为空")
        @JsonProperty("registerType")
        RegisterType registerType,

        @NotBlank(message = "邮箱不能为空")
        @ValidEmail(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "密码不能为空")
        @ValidPassword(message = "密码必须包含大小写字母、数字、特殊字符，且长度在8-16个字符之间")
        String password,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^[0-9]{6}$", message = "验证码必须是6位数字")
        String verificationCode,

        @JsonProperty("userDomain")
        UserDomain userDomain
) implements RegisterRequest {
}

