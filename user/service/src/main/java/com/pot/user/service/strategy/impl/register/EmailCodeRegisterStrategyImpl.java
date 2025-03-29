package com.pot.user.service.strategy.impl.register;

import com.pot.user.service.controller.request.register.EmailCodeRegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.enums.SendCodeChannelType;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/28 23:42
 * @description: 邮件验证码注册策略实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailCodeRegisterStrategyImpl extends AbstractRegisterStrategyImpl<EmailCodeRegisterRequest> {

    private final VerificationCodeStrategyFactory verificationCodeStrategyFactory;


    @Override
    public LoginRegisterType getRegisterType() {
        return LoginRegisterType.PHONE_CODE;
    }

    @Override
    protected void checkUniqueness(EmailCodeRegisterRequest request) {
        checkUnique(User::getEmail, request.getEmail());
    }

    @Override
    protected void checkCodeIfNeeded(EmailCodeRegisterRequest request) {
        verificationCodeStrategyFactory.getStrategy(SendCodeChannelType.EMAIL).validateCode(request.getEmail(), request.getCode());
    }

    @Override
    protected User buildUser(EmailCodeRegisterRequest request) {
        String email = request.getEmail();
        String name = generateRandomNickname();
        String password = generateRandomPassword();
        return createBaseBuilder()
                .email(email)
                .name(name)
                .nickname(name)
                .password(password)
                .build();
    }
}
