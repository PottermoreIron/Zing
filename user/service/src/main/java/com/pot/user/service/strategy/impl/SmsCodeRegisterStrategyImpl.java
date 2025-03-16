package com.pot.user.service.strategy.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.pot.user.service.controller.request.RegisterRequest;
import com.pot.user.service.controller.request.SmsCodeRegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.RegisterType;
import com.pot.user.service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/3/11 23:44
 * @description: 手机验证码注册策略实现类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmsCodeRegisterStrategyImpl extends AbstractRegisterStrategyImpl {

    private final UserService userService;

    @Override
    protected void checkUniqueness(RegisterRequest request) {
        // todo 校验手机号是否已注册
        String phone = ((SmsCodeRegisterRequest) request).getPhone();
        String code = ((SmsCodeRegisterRequest) request).getCode();
        LambdaQueryChainWrapper<User> query = userService.lambdaQuery().eq(User::getPhone, phone);
        if (query.count() > 0) {
            throw new IllegalArgumentException("手机号已注册");
        }
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
    public RegisterType getRegisterType() {
        return RegisterType.PHONE_CODE;
    }

    @Override
    public boolean supports(RegisterType type) {
        return type.equals(RegisterType.PHONE_CODE);
    }

    @Override
    public void validate(RegisterRequest request) {
        // todo 校验手机号
        // todo 校验验证码
    }
}
