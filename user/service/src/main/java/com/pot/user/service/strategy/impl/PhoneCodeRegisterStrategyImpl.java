package com.pot.user.service.strategy.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.pot.common.enums.ResultCode;
import com.pot.user.service.controller.request.register.PhoneCodeRegisterRequest;
import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.controller.response.Tokens;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.RegisterType;
import com.pot.user.service.exception.BusinessException;
import com.pot.user.service.service.SmsCodeService;
import com.pot.user.service.service.UserService;
import com.pot.user.service.utils.JwtUtils;
import com.pot.user.service.utils.PasswordUtils;
import com.pot.user.service.utils.RandomStringGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author: Pot
 * @created: 2025/3/11 23:44
 * @description: 手机验证码注册策略实现类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PhoneCodeRegisterStrategyImpl extends AbstractRegisterStrategyImpl {

    private final UserService userService;

    private final SmsCodeService smsCodeService;

    @Override
    protected void checkUniqueness(RegisterRequest request) {
        String phone = ((PhoneCodeRegisterRequest) request).getPhone();
        LambdaQueryChainWrapper<User> query = userService.lambdaQuery().eq(User::getPhone, phone);
        User user = query.one();
        if (ObjectUtils.isNotEmpty(user)) {
            throw new BusinessException(ResultCode.USER_EXIST);
        }
    }

    @Override
    protected void checkCodeIfNeeded(RegisterRequest request) {
        String phone = ((PhoneCodeRegisterRequest) request).getPhone();
        String code = ((PhoneCodeRegisterRequest) request).getCode();
        smsCodeService.validateSmsCode(phone, code);
    }

    @Override
    protected Tokens doRegister(RegisterRequest request) {
        User user = createDefaultUser(request);
        userService.save(user);
        Long uid = user.getUid();
        return Tokens.builder()
                .accessToken(JwtUtils.createAccessToken(uid))
                .refreshToken(JwtUtils.createRefreshToken(uid))
                .build();
    }

    @Override
    public RegisterType getRegisterType() {
        return RegisterType.PHONE_CODE;
    }

    @Override
    public boolean supports(RegisterType type) {
        return type.equals(RegisterType.PHONE_CODE);
    }

    @Override
    protected void validate(RegisterRequest request) {
        // 用validation校验了手机号和验证码
    }

    private User createDefaultUser(RegisterRequest request) {
        // 帮我用builder创建一个默认用户, 你可以在这里设置一些默认值
        String phone = ((PhoneCodeRegisterRequest) request).getPhone();
        String name = "User_%s".formatted(RandomStringGenerator.generateRandomString());
        Long uid = getNextId();
        String defaultPassword = PasswordUtils.generateDefaultPassword();
        LocalDateTime registerTime = LocalDateTime.now();
        return User.builder()
                .phone(phone)
                .nickname(name)
                .name(name)
                .uid(uid)
                .password(defaultPassword)
                .registerTime(registerTime)
                .status(0)
                .deleted(false)
                .build();
    }
}
