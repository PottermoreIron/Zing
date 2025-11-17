package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pot.auth.domain.registration.enums.RegistrationType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @author: Pot
 * @created: 2025/11/17 23:20
 * @description: 邮箱-密码注册
 */
@JsonTypeName("EMAIL_PASSWORD")
public record EmailPasswordRegisterRequest(
        RegistrationType registrationType,
        UserDomain userDomain,

        @NotBlank(message = "邮箱不能为空")
        @ValidEmail(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "密码不能为空")
        @ValidPassword(message = "密码必须包含大小写字母, 数字, 特殊字符，且长度在8-16个字符之间")
        String password,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
        String verificationCode
) implements RegisterRequest {

    public EmailPasswordRegisterRequest {
        if (registrationType == null) {
            registrationType = RegistrationType.EMAIL_PASSWORD;
        }
    }
}
