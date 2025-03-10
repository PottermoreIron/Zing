package com.pot.user.service.strategy.impl;

import com.pot.user.service.enums.RegisterType;
import com.pot.user.service.strategy.RegisterStrategy;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/3/10 23:22
 * @description: 验证码注册
 */
public class SmsCodeRegisterStrategy implements RegisterStrategy {

    @Override
    public RegisterType getStrategy() {
        return RegisterType.PHONE_CODE;
    }

    @Override
    public boolean validate(Map<String, String> params) {
        // todo 校验参数
        return true;
    }

    @Override
    public void doRegister(Map<String, String> params) {
        // todo 注册逻辑
    }
}
