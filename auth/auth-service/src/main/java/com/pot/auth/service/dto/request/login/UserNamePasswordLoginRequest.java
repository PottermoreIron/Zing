package com.pot.auth.service.dto.request.login;

import com.pot.auth.service.enums.LoginType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/8 23:20
 * @description: 用户名密码登录请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserNamePasswordLoginRequest extends LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;

    public UserNamePasswordLoginRequest() {
        this.type = LoginType.USERNAME_PASSWORD;
    }
}
