package com.pot.user.service.strategy.impl;

import com.pot.common.enums.ResultCode;
import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.controller.request.SendCodeRequest;
import com.pot.user.service.controller.request.SendPhoneCodeRequest;
import com.pot.user.service.enums.SendCodeChannelType;
import com.pot.user.service.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/27 23:55
 * @description: 手机验证码策略实现
 */
@Service
@Slf4j
public class PhoneSendCodeStrategyImpl extends AbstractSendCodeStrategyImpl {
    @Override
    protected void doSend(String target, String code) {
        // todo 发送验证码
        // 这里可以调用短信服务发送验证码
        System.out.println("Sending SMS code: " + code + " to " + target);
    }

    @Override
    protected void checkTarget(String phone) {
        if (!ValidationUtils.isPhone(phone)) {
            throw new BusinessException(ResultCode.PHONE_NOT_LEGAL);
        }
    }

    @Override
    protected void setTarget(SendCodeRequest request) {
        this.target = ((SendPhoneCodeRequest) request).getPhone();
    }

    @Override
    public SendCodeChannelType getVerificationCodeType() {
        return SendCodeChannelType.PHONE;
    }
}
