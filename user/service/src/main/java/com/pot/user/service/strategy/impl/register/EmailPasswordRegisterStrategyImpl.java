package com.pot.user.service.strategy.impl.register;

import com.pot.user.service.controller.request.register.EmailPasswordRegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/28 23:34
 * @description: 邮箱密码注册策略实现类
 */
@Service
@Slf4j
public class EmailPasswordRegisterStrategyImpl extends AbstractRegisterStrategyImpl<EmailPasswordRegisterRequest> {

    @Override
    public LoginRegisterType getRegisterType() {
        return LoginRegisterType.EMAIL_PASSWORD;
    }

    @Override
    protected void checkUniqueness(EmailPasswordRegisterRequest request) {
        checkUnique(User::getEmail, request.getEmail());
    }

    @Override
    protected User buildUser(EmailPasswordRegisterRequest request) {
        String email = request.getEmail();
        String name = generateRandomNickname();
        String password = generateEncodedPassword(request.getPassword());
        return createBaseBuilder()
                .email(email)
                .name(name)
                .nickname(name)
                .password(password)
                .build();
    }
}