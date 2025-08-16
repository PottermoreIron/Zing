package com.pot.user.service.strategy.impl.register;

import com.pot.common.utils.IdUtils;
import com.pot.common.utils.PasswordUtils;
import com.pot.user.service.controller.request.register.PhoneCodeRegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterEnum;
import com.pot.user.service.enums.SendCodeChannelEnum;
import com.pot.user.service.service.UserService;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/11 23:44
 * @description: 手机验证码注册策略实现类
 */
@Service
@Slf4j
public class PhoneCodeRegisterStrategyImpl extends AbstractRegisterStrategyImpl<PhoneCodeRegisterRequest> {

    public PhoneCodeRegisterStrategyImpl(UserService userService, VerificationCodeStrategyFactory verificationCodeStrategyFactory, PasswordUtils passwordUtils, IdUtils idUtils) {
        super(userService, verificationCodeStrategyFactory, passwordUtils, idUtils);
    }

    @Override
    public LoginRegisterEnum getRegisterType() {
        return LoginRegisterEnum.PHONE_CODE;
    }

    @Override
    protected void checkUniqueness(PhoneCodeRegisterRequest request) {
        checkUnique(User::getPhone, request.getPhone());
    }

    @Override
    protected void checkCodeIfNeeded(PhoneCodeRegisterRequest request) {
        verificationCodeStrategyFactory.getStrategy(SendCodeChannelEnum.PHONE).validateCode(request.getPhone(), request.getCode());
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
