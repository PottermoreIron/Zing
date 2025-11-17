package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pot.auth.domain.registration.enums.RegistrationType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @author: Pot
 * @created: 2025/11/17 23:20
 * @description: 手机号-验证码注册
 */
@JsonTypeName("PHONE_CODE")
public record PhoneCodeRegisterRequest(
        RegistrationType registrationType,
        UserDomain userDomain,

        @NotBlank(message = "手机号不能为空")
        @ValidPhone(message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
        String verificationCode
) implements RegisterRequest {

    public PhoneCodeRegisterRequest {
        if (registrationType == null) {
            registrationType = RegistrationType.PHONE_CODE;
        }
    }
}
