package com.pot.user.service.controller.request;

import com.pot.common.utils.ValidationUtils;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/3/19 23:41
 * @description: 发送手机验证码请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SendSmsCodeRequest extends SendCodeRequest {
    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "Phone number format is incorrect")
    String phone;
}
