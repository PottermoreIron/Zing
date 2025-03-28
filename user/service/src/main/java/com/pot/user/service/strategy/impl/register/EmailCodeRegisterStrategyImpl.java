package com.pot.user.service.strategy.impl.register;

import com.pot.common.enums.ResultCode;
import com.pot.user.service.controller.request.register.EmailCodeRegisterRequest;
import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.enums.SendCodeChannelType;
import com.pot.user.service.exception.BusinessException;
import com.pot.user.service.service.UserService;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import com.pot.user.service.utils.PasswordUtils;
import com.pot.user.service.utils.RandomStringGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author: Pot
 * @created: 2025/3/28 23:42
 * @description: 邮件验证码注册策略实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailCodeRegisterStrategyImpl extends AbstractRegisterStrategyImpl {

    private final UserService userService;
    private final VerificationCodeStrategyFactory verificationCodeStrategyFactory;

    @Override
    protected void checkUniqueness(RegisterRequest request) {
        String email = ((EmailCodeRegisterRequest) request).getEmail();
        User user = userService.lambdaQuery().eq(User::getEmail, email).one();
        if (ObjectUtils.isNotEmpty(user)) {
            throw new BusinessException(ResultCode.USER_EXIST);
        }
    }

    @Override
    protected void checkCodeIfNeeded(RegisterRequest request) {
        String email = ((EmailCodeRegisterRequest) request).getEmail();
        String code = ((EmailCodeRegisterRequest) request).getCode();
        verificationCodeStrategyFactory.getStrategy(SendCodeChannelType.EMAIL).validateCode(email, code);
    }

    @Override
    protected Long doRegister(RegisterRequest request) {
        User user = createDefaultUser(request);
        userService.save(user);
        return user.getUid();
    }

    @Override
    public LoginRegisterType getRegisterType() {
        return LoginRegisterType.PHONE_CODE;
    }

    @Override
    public boolean supports(LoginRegisterType type) {
        return type.equals(LoginRegisterType.PHONE_CODE);
    }

    @Override
    protected void validate(RegisterRequest request) {
        // 用validation校验了手机号和验证码
    }

    private User createDefaultUser(RegisterRequest request) {
        // 创建一个默认用户
        String email = ((EmailCodeRegisterRequest) request).getEmail();
        String name = "User_%s".formatted(RandomStringGenerator.generateRandomString());
        Long uid = getNextId();
        String defaultPassword = PasswordUtils.generateDefaultPassword();
        LocalDateTime registerTime = LocalDateTime.now();
        return User.builder()
                .nickname(name)
                .name(name)
                .uid(uid)
                .password(defaultPassword)
                .email(email)
                .registerTime(registerTime)
                .status(0)
                .deleted(false)
                .build();
    }
}
