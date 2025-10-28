package com.pot.auth.service.dto.request.register;

import com.pot.auth.service.enums.RegisterType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/3 23:53
 * @description: 邮箱验证码注册请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EmailCodeRegisterRequest extends RegisterRequest {
    @Email
    private String email;
    @Size(min = 6, max = 6, message = "Captcha length is 6 digits")
    private String code;

    public EmailCodeRegisterRequest() {
        this.type = RegisterType.EMAIL_CODE;
    }
}
