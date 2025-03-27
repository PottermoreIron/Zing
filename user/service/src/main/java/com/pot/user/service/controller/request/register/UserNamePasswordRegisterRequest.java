package com.pot.user.service.controller.request.register;

import com.pot.common.utils.ValidationUtils;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/3/25 23:58
 * @description: 用户名密码注册
 */
@Data
public class UserNamePasswordRegisterRequest {
    @Pattern(regexp = ValidationUtils.NICKNAME_REGEX, message = "Nick name can only contain letters, numbers, underscores and chinese characters. The length is 1-30 characters")
    private String username;
    @Pattern(regexp = ValidationUtils.PASSWORD_REGEX, message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character. The length is 8-16 characters")
    private String password;
}
