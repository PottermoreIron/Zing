package com.pot.auth.service.dto.request.register;

import com.pot.auth.service.enums.RegisterType;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/3 23:51
 * @description: 邮箱密码注册请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EmailPasswordRegisterRequest extends RegisterRequest {
    @Email
    private String email;
    @Pattern(regexp = ValidationUtils.PASSWORD_REGEX, message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character. The length is 8-16 characters")
    private String password;

    public EmailPasswordRegisterRequest() {
        this.type = RegisterType.EMAIL_PASSWORD;
    }
}
