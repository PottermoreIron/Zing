package com.pot.user.service.strategy.impl.register;

import com.pot.user.service.controller.request.register.PhonePasswordRegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.service.UserService;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import com.sankuai.inf.leaf.service.SegmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/28 23:34
 * @description: 手机密码注册策略实现类
 */
@Service
@Slf4j
public class PhonePasswordRegisterStrategyImpl extends AbstractRegisterStrategyImpl<PhonePasswordRegisterRequest> {

    public PhonePasswordRegisterStrategyImpl(UserService userService, SegmentService segmentService, VerificationCodeStrategyFactory verificationCodeStrategyFactory, PasswordEncoder passwordEncoder) {
        super(userService, segmentService, verificationCodeStrategyFactory, passwordEncoder);
    }

    @Override
    public LoginRegisterType getRegisterType() {
        return LoginRegisterType.PHONE_PASSWORD;
    }

    @Override
    protected void checkUniqueness(PhonePasswordRegisterRequest request) {
        checkUnique(User::getPhone, request.getPhone());
    }

    @Override
    protected User buildUser(PhonePasswordRegisterRequest request) {
        String phone = request.getPhone();
        String name = generateRandomNickname();
        String password = generateEncodedPassword(request.getPassword());
        return createBaseBuilder()
                .phone(phone)
                .name(name)
                .nickname(name)
                .password(password)
                .build();
    }
}
