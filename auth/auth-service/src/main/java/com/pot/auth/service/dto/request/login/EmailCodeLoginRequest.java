package com.pot.auth.service.dto.request.login;

import com.pot.auth.service.enums.LoginType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/8 23:22
 * @description: 邮箱验证码登录请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EmailCodeLoginRequest extends LoginRequest {
    @Email(message = "邮箱格式不正确")
    private String email;
    @Size(min = 6, max = 6, message = "验证码长度为6位")
    private String code;

    public EmailCodeLoginRequest() {
        this.type = LoginType.EMAIL_CODE;
    }
}
