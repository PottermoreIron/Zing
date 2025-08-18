package com.pot.member.service.controller.request.register;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/3/25 23:54
 * @description: 邮箱验证码注册请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EmailCodeRegisterRequest extends RegisterRequest {
    public EmailCodeRegisterRequest() {
        this.type = 5;
    }

    @Email
    private String email;
    @Size(min = 6, max = 6, message = "Captcha length is 6 digits")
    private String code;
}
