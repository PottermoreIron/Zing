package com.pot.user.service.strategy.impl;

import com.pot.user.service.controller.request.RegisterRequest;
import com.pot.user.service.enums.RegisterType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/3/11 23:44
 * @description: 手机验证码注册策略实现类
 */
@Component
@Slf4j
public class SmsCodeRegisterStrategyImpl extends AbstractRegisterStrategyImpl {
    @Override
    protected void checkUniqueness(RegisterRequest request) {
        log.info("sms-code checkUniqueness request={}", request);
    }

    @Override
    protected void sendVerificationIfNeeded(RegisterRequest request) {
        log.info("sms-code sendVerificationIfNeeded request={}", request);
    }

    @Override
    protected void doRegister(RegisterRequest request) {
        log.info("sms-code doRegister request={}", request);
    }

    @Override
    public boolean supports(RegisterType type) {
        return type.equals(RegisterType.PHONE_CODE);
    }

    @Override
    public void validate(RegisterRequest request) {
        log.info("sms-code validate request={}", request);
    }
}
