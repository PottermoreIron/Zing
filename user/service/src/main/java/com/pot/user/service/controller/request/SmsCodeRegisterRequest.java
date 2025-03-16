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
    @IsMobile
    private String phone;
    @Size(min = 6, max = 6, message = "验证码长度为 5 位")
    private String code;
}
