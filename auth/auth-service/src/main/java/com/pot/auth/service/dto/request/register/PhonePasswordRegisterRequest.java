package com.pot.auth.service.dto.request.register;

import com.pot.auth.service.enums.RegisterType;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/3 23:50
 * @description: 手机号密码注册请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PhonePasswordRegisterRequest extends RegisterRequest {
    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "Phone number format is incorrect")
    private String phone;
    @Pattern(regexp = ValidationUtils.PASSWORD_REGEX, message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character. The length is 8-16 characters")
    private String password;

    public PhonePasswordRegisterRequest() {
        this.type = RegisterType.PHONE_PASSWORD;
    }
}
