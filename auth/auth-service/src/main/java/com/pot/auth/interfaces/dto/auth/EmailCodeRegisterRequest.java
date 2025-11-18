package com.pot.auth.interfaces.dto.auth;

import com.pot.auth.domain.validation.annotations.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 邮箱验证码注册请求
 *
 * @author yecao
 * @since 2025-11-18
 */
public record EmailCodeRegisterRequest(
        @NotBlank(message = "注册类型不能为空")
        String registerType,

        @NotBlank(message = "邮箱不能为空")
        @ValidEmail(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^[0-9]{6}$", message = "验证码必须是6位数字")
        String verificationCode,

        @Size(max = 20, message = "用户域长度不能超过20个字符")
        String userDomain
) implements RegisterRequest {
}

