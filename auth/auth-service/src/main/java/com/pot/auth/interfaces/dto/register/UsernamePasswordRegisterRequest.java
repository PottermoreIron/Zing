package com.pot.auth.interfaces.dto.register;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pot.auth.domain.registration.enums.RegistrationType;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import com.pot.auth.domain.validation.annotations.ValidUsername;
import jakarta.validation.constraints.NotBlank;

/**
 * @author: Pot
 * @created: 2025/11/17 23:18
 * @description: 用户名-密码注册请求
 */
@JsonTypeName("USERNAME_PASSWORD")
public record UsernamePasswordRegisterRequest(
        RegistrationType registrationType,
        UserDomain userDomain,

        @NotBlank(message = "用户名不能为空")
        @ValidUsername(message = "用户名只允许中文、字母、数字、下划线，连字符，长度在1-30个字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @ValidPassword(message = "密码必须包含大小写字母, 数字, 特殊字符，且长度在8-16个字符之间")
        String password
) implements RegisterRequest {

    public UsernamePasswordRegisterRequest {
        if (registrationType == null) {
            registrationType = RegistrationType.USERNAME_PASSWORD;
        }
    }
}
