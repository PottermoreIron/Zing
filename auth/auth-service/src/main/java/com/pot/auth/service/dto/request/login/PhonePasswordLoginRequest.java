package com.pot.auth.service.dto.request.login;

import com.pot.auth.service.enums.LoginType;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/8 23:21
 * @description: 手机密码登录请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PhonePasswordLoginRequest extends LoginRequest {
    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;
    @NotBlank(message = "密码不能为空")
    private String password;

    public PhonePasswordLoginRequest() {
        this.type = LoginType.PHONE_PASSWORD;
    }
}
