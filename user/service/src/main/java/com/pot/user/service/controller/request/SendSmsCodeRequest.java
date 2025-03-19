package com.pot.user.service.controller.request;

import com.pot.user.service.annotations.IsMobile;
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
    @IsMobile
    String phone;
}
