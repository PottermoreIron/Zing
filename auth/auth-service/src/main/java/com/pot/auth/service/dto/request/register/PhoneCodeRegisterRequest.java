package com.pot.auth.service.dto.request.register;

import com.pot.auth.service.enums.RegisterType;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/3 23:52
 * @description: 手机验证码注册请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PhoneCodeRegisterRequest extends RegisterRequest {
    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "Phone number format is incorrect")
    private String phone;
    @Size(min = 6, max = 6, message = "Captcha length is 6 digits")
    private String code;

    public PhoneCodeRegisterRequest() {
        this.type = RegisterType.PHONE_CODE;
    }
}
