package com.pot.auth.service.dto.request.login;

import com.pot.auth.service.enums.LoginType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/8 23:21
 * @description: 邮箱密码登录请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EmailPasswordLoginRequest extends LoginRequest {
    @Email(message = "邮箱格式不正确")
    private String email;
    @NotBlank(message = "密码不能为空")
    private String password;

    public EmailPasswordLoginRequest() {
        this.type = LoginType.EMAIL_PASSWORD;
    }
}
