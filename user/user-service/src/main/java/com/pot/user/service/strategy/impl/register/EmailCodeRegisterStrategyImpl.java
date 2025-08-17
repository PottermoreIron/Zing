package com.pot.user.service.strategy.impl.register;

import com.pot.common.id.IdService;
import com.pot.user.service.controller.request.register.EmailCodeRegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterEnum;
import com.pot.user.service.enums.SendCodeChannelEnum;
import com.pot.user.service.service.UserService;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/28 23:42
 * @description: 邮件验证码注册策略实现类
 */
@Service
@Slf4j
public class EmailCodeRegisterStrategyImpl extends AbstractRegisterStrategyImpl<EmailCodeRegisterRequest> {

    public EmailCodeRegisterStrategyImpl(UserService userService, VerificationCodeStrategyFactory verificationCodeStrategyFactory, IdService idService) {
        super(userService, verificationCodeStrategyFactory, idService);
    }

    @Override
    public LoginRegisterEnum getRegisterType() {
        return LoginRegisterEnum.EMAIL_CODE;
    }

    @Override
    protected void checkUniqueness(EmailCodeRegisterRequest request) {
        checkUnique(User::getEmail, request.getEmail());
    }

    @Override
    protected void checkCodeIfNeeded(EmailCodeRegisterRequest request) {
        verificationCodeStrategyFactory.getStrategy(SendCodeChannelEnum.EMAIL).validateCode(request.getEmail(), request.getCode());
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
