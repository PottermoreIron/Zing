package com.pot.member.service.controller.request.register;

import com.pot.common.utils.ValidationUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/3/25 23:59
 * @description: 邮箱密码注册
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EmailPasswordRegisterRequest extends RegisterRequest {
    public EmailPasswordRegisterRequest() {
        this.type = 3;
    }

    @Email
    private String email;
    @Pattern(regexp = ValidationUtils.PASSWORD_REGEX, message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character. The length is 8-16 characters")
    private String password;
}
