package com.pot.member.service.controller.request.register;

import com.pot.common.utils.ValidationUtils;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/3/16 22:06
 * @description: 手机验证码注册请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PhoneCodeRegisterRequest extends RegisterRequest {
    public PhoneCodeRegisterRequest() {
        this.type = 4;
    }

    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "Phone number format is incorrect")
    private String phone;
    @Size(min = 6, max = 6, message = "Captcha length is 6 digits")
    private String code;
}
