package com.pot.auth.service.dto.request.login;

import com.pot.auth.service.enums.LoginType;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/9/8 23:21
 * @description: 手机验证码登录请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PhoneCodeLoginRequest extends LoginRequest {
    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;
    @Size(min = 6, max = 6, message = "验证码长度为6位")
    private String code;

    public PhoneCodeLoginRequest() {
        this.type = LoginType.PHONE_CODE;
    }
}
