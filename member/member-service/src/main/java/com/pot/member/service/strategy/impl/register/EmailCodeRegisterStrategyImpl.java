package com.pot.member.service.strategy.impl.register;

import com.pot.common.id.IdService;
import com.pot.member.service.controller.request.register.EmailCodeRegisterRequest;
import com.pot.member.service.entity.User;
import com.pot.member.service.enums.LoginRegisterEnum;
import com.pot.member.service.enums.SendCodeChannelEnum;
import com.pot.member.service.service.UserService;
import com.pot.member.service.strategy.factory.VerificationCodeStrategyFactory;
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
