package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pot.auth.domain.registration.enums.RegistrationType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import com.pot.auth.domain.validation.annotations.ValidPhone;
import jakarta.validation.constraints.NotBlank;

/**
 * @author: Pot
 * @created: 2025/11/17 23:19
 * @description: 手机号-密码注册
 */
@JsonTypeName("PHONE_PASSWORD")
public record PhonePasswordRegisterRequest(
        RegistrationType registrationType,
        UserDomain userDomain,

        @NotBlank(message = "手机号不能为空")
        @ValidPhone(message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "密码不能为空")
        @ValidPassword(message = "密码必须包含大小写字母, 数字, 特殊字符，且长度在8-16个字符之间")
        String password
) implements RegisterRequest {

    public PhonePasswordRegisterRequest {
        if (registrationType == null) {
            registrationType = RegistrationType.PHONE_PASSWORD;
        }

    }
}