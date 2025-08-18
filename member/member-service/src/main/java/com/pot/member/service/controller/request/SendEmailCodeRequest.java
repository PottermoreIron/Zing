package com.pot.member.service.controller.request;

import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/3/28 22:41
 * @description: 发送邮件验证码请求类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SendEmailCodeRequest extends SendCodeRequest {
    @Email
    String email;
}
