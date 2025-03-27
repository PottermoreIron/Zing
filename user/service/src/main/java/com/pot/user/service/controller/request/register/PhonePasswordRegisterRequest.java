package com.pot.user.service.controller.request.register;

import com.pot.common.utils.ValidationUtils;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/3/26 00:00
 * @description: 手机号密码注册
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PhonePasswordRegisterRequest extends RegisterRequest {
    public PhonePasswordRegisterRequest() {
        this.type = 2;
    }

    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "Phone number format is incorrect")
    private String phone;
    @Pattern(regexp = ValidationUtils.PASSWORD_REGEX, message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character. The length is 8-16 characters")
    private String password;
}
