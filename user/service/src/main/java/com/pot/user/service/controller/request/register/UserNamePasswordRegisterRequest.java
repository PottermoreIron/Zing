package com.pot.user.service.controller.request.register;

import com.pot.user.service.annotations.IsNickName;
import com.pot.user.service.annotations.IsPassword;
import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/3/25 23:58
 * @description: 用户名密码注册
 */
@Data
public class UserNamePasswordRegisterRequest {
    @IsNickName
    private String username;
    @IsPassword
    private String password;
}
