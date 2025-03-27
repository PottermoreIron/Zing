package com.pot.user.service.strategy.impl;

import com.pot.common.enums.ResultCode;
import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.exception.BusinessException;

/**
 * @author: Pot
 * @created: 2025/3/27 23:55
 * @description: 手机验证码策略实现
 */
public class SmsCodeStrategyImpl extends AbstractVerificationCodeStrategyImpl {
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
}
