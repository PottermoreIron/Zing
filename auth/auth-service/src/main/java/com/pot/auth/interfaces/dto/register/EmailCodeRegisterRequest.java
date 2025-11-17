package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pot.auth.domain.registration.enums.RegistrationType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @author: Pot
 * @created: 2025/11/17 23:21
 * @description: 邮箱-验证码注册
 */
@JsonTypeName("EMAIL_CODE")
public record EmailCodeRegisterRequest(
        RegistrationType registrationType,
        UserDomain userDomain,

        @NotBlank(message = "邮箱不能为空")
        @ValidEmail(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
        String verificationCode
) implements RegisterRequest {

    public EmailCodeRegisterRequest {
        if (registrationType == null) {
            registrationType = RegistrationType.EMAIL_CODE;
        }
    }
}
