package com.pot.auth.service.dto.request.register;

import com.pot.auth.service.enums.RegisterType;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/3 23:49
 * @description: 用户名密码注册请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserNamePasswordRegisterRequest extends RegisterRequest {
    @Pattern(regexp = ValidationUtils.NICKNAME_REGEX, message = "Nick name can only contain letters, numbers, underscores and chinese characters. The length is 1-30 characters")
    private String username;
    @Pattern(regexp = ValidationUtils.PASSWORD_REGEX, message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character. The length is 8-16 characters")
    private String password;

    public UserNamePasswordRegisterRequest() {
        this.type = RegisterType.USERNAME_PASSWORD;
    }
}
