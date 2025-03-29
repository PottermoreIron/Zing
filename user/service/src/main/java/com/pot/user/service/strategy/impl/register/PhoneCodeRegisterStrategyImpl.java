package com.pot.user.service.strategy.impl.register;

import com.pot.user.service.controller.request.register.PhoneCodeRegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.enums.SendCodeChannelType;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/11 23:44
 * @description: 手机验证码注册策略实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneCodeRegisterStrategyImpl extends AbstractRegisterStrategyImpl<PhoneCodeRegisterRequest> {
    private final VerificationCodeStrategyFactory verificationCodeStrategyFactory;

    @Override
    public LoginRegisterType getRegisterType() {
        return LoginRegisterType.PHONE_CODE;
    }

    @Override
    protected void checkUniqueness(PhoneCodeRegisterRequest request) {
        checkUnique(User::getPhone, request.getPhone());
    }

    @Override
    protected void checkCodeIfNeeded(PhoneCodeRegisterRequest request) {
        verificationCodeStrategyFactory.getStrategy(SendCodeChannelType.PHONE).validateCode(request.getPhone(), request.getCode());
    }

    @Override
    protected User buildUser(PhoneCodeRegisterRequest request) {
        String phone = request.getPhone();
        String name = generateRandomNickname();
        String password = generateRandomPassword();
        return createBaseBuilder()
                .phone(phone)
                .name(name)
                .nickname(name)
                .password(password)
                .build();
    }
}
