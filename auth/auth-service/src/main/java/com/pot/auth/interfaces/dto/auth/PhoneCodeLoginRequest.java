package com.pot.auth.interfaces.dto.auth;

import com.pot.auth.domain.validation.annotations.ValidPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 手机号验证码登录请求
 *
 * @author yecao
 * @since 2025-11-18
 */
public record PhoneCodeLoginRequest(
        @NotBlank(message = "登录类型不能为空")
        String loginType,

        @NotBlank(message = "手机号不能为空")
        @ValidPhone(message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^[0-9]{6}$", message = "验证码必须是6位数字")
        String verificationCode,

        @Size(max = 20, message = "用户域长度不能超过20个字符")
        String userDomain
) implements LoginRequest {
}

