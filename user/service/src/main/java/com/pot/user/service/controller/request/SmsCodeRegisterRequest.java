package com.pot.user.service.controller.request;

import com.pot.user.service.annotations.IsMobile;
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
public class SmsCodeRegisterRequest extends RegisterRequest {
    public SmsCodeRegisterRequest() {
        this.type = 4;
    }

    @IsMobile
    private String phone;
    @Size(min = 6, max = 6, message = "Captcha length is 6 digits")
    private String code;

    private Boolean needCheckCode = true;
}
