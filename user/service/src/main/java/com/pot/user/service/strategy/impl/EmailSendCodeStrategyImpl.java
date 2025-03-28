package com.pot.user.service.strategy.impl;

import com.pot.common.enums.ResultCode;
import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.controller.request.SendCodeRequest;
import com.pot.user.service.controller.request.SendEmailCodeRequest;
import com.pot.user.service.enums.SendCodeChannelType;
import com.pot.user.service.exception.BusinessException;

/**
 * @author: Pot
 * @created: 2025/3/28 22:51
 * @description: 发送邮件验证码策略实现
 */
public class EmailSendCodeStrategyImpl extends AbstractSendCodeStrategyImpl {
    @Override
    protected void doSend(String target, String code) {
        // todo 发送验证码
        // 这里可以调用邮件服务发送验证码
        System.out.println("Sending Email code: " + code + " to " + target);
    }

    @Override
    protected void checkTarget(String email) {
        if (!ValidationUtils.isEmail(email)) {
            throw new BusinessException(ResultCode.EMAIL_NOT_LEGAL);
        }
    }

    @Override
    protected void setTarget(SendCodeRequest request) {
        this.target = ((SendEmailCodeRequest) request).getEmail();
    }

    @Override
    public SendCodeChannelType getVerificationCodeType() {
        return SendCodeChannelType.EMAIL;
    }
}
