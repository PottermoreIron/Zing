package com.pot.user.service.strategy.impl.register;

import com.pot.common.enums.ResultCode;
import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.controller.request.register.UserNamePasswordRegisterRequest;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.exception.BusinessException;
import com.pot.user.service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author: Pot
 * @created: 2025/3/28 23:12
 * @description: 用户密码注册策略实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsernamePasswordRegisterStrategyImpl extends AbstractRegisterStrategyImpl {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void checkUniqueness(RegisterRequest request) {
        String username = ((UserNamePasswordRegisterRequest) request).getUsername();
        User user = userService.lambdaQuery().eq(User::getNickname, username).one();
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
        return LoginRegisterType.USERNAME_PASSWORD;
    }

    @Override
    public boolean supports(LoginRegisterType type) {
        return type.equals(LoginRegisterType.USERNAME_PASSWORD);
    }

    @Override
    protected void validate(RegisterRequest request) {
        // 用validation校验了用户名和密码
    }

    private User createDefaultUser(RegisterRequest request) {
        // 创建一个默认用户
        String name = ((UserNamePasswordRegisterRequest) request).getUsername();
        String rawPassword = ((UserNamePasswordRegisterRequest) request).getPassword();
        String password = passwordEncoder.encode(rawPassword);
        Long uid = getNextId();
        LocalDateTime registerTime = LocalDateTime.now();
        return User.builder()
                .nickname(name)
                .name(name)
                .uid(uid)
                .password(password)
                .registerTime(registerTime)
                .status(0)
                .deleted(false)
                .build();
    }
}
