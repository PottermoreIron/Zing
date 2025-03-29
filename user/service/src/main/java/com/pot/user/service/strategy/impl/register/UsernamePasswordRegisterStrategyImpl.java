package com.pot.user.service.strategy.impl.register;

import com.pot.user.service.controller.request.register.UserNamePasswordRegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/28 23:12
 * @description: 用户密码注册策略实现类
 */
@Service
@Slf4j
public class UsernamePasswordRegisterStrategyImpl extends AbstractRegisterStrategyImpl<UserNamePasswordRegisterRequest> {

    @Override
    public LoginRegisterType getRegisterType() {
        return LoginRegisterType.USERNAME_PASSWORD;
    }

    @Override
    protected void checkUniqueness(UserNamePasswordRegisterRequest request) {
        checkUnique(User::getName, request.getUsername());
    }

    @Override
    protected User buildUser(UserNamePasswordRegisterRequest request) {
        String name = request.getUsername();
        String password = generateEncodedPassword(request.getPassword());
        return createBaseBuilder()
                .name(name)
                .nickname(name)
                .password(password)
                .build();
    }
}
