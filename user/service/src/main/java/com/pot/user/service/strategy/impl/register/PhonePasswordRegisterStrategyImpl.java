package com.pot.user.service.strategy.impl.register;

import com.pot.common.enums.ResultCode;
import com.pot.user.service.controller.request.register.PhonePasswordRegisterRequest;
import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.exception.BusinessException;
import com.pot.user.service.service.UserService;
import com.pot.user.service.utils.RandomStringGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author: Pot
 * @created: 2025/3/28 23:34
 * @description: 手机密码注册策略实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhonePasswordRegisterStrategyImpl extends AbstractRegisterStrategyImpl {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void checkUniqueness(RegisterRequest request) {
        String phone = ((PhonePasswordRegisterRequest) request).getPhone();
        User user = userService.lambdaQuery().eq(User::getPhone, phone).one();
        if (ObjectUtils.isNotEmpty(user)) {
            throw new BusinessException(ResultCode.USER_EXIST);
        }
    }

    @Override
    protected void checkCodeIfNeeded(RegisterRequest request) {
    }

    @Override
    protected Long doRegister(RegisterRequest request) {
        User user = createDefaultUser(request);
        userService.save(user);
        return user.getUid();
    }


    @Override
    public LoginRegisterType getRegisterType() {
        return LoginRegisterType.PHONE_PASSWORD;
    }

    @Override
    public boolean supports(LoginRegisterType type) {
        return type.equals(LoginRegisterType.PHONE_PASSWORD);
    }

    @Override
    protected void validate(RegisterRequest request) {
        // 用validation校验了手机和密码
    }

    private User createDefaultUser(RegisterRequest request) {
        // 创建一个默认用户
        String phone = ((PhonePasswordRegisterRequest) request).getPhone();
        String name = "User_%s".formatted(RandomStringGenerator.generateRandomString());
        String rawPassword = ((PhonePasswordRegisterRequest) request).getPassword();
        String password = passwordEncoder.encode(rawPassword);
        Long uid = getNextId();
        LocalDateTime registerTime = LocalDateTime.now();
        return User.builder()
                .nickname(name)
                .name(name)
                .uid(uid)
                .phone(phone)
                .password(password)
                .registerTime(registerTime)
                .status(0)
                .deleted(false)
                .build();
    }
}
