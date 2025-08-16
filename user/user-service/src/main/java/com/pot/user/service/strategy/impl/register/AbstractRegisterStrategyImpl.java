package com.pot.user.service.strategy.impl.register;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.pot.common.enums.ResultCode;
import com.pot.common.exception.BusinessException;
import com.pot.common.utils.IdUtils;
import com.pot.common.utils.PasswordUtils;
import com.pot.common.utils.RandomUtils;
import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.controller.response.Tokens;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.IdBizEnum;
import com.pot.user.service.service.UserService;
import com.pot.user.service.strategy.RegisterStrategy;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import com.pot.user.service.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author: Pot
 * @created: 2025/3/10 23:22
 * @description: 抽象注册策略类
 */
@Service
@RequiredArgsConstructor
public abstract class AbstractRegisterStrategyImpl<T extends RegisterRequest> implements RegisterStrategy<T> {
    protected final UserService userService;
    protected final VerificationCodeStrategyFactory verificationCodeStrategyFactory;
    protected final PasswordUtils passwordUtils;
    protected final IdUtils idUtils;

    @Override
    public Tokens register(T request) {
        // 校验参数
        validate(request);
        // 校验唯一性
        checkUniqueness(request);
        // 校验验证码
        checkCodeIfNeeded(request);
        // 注册
        doRegister(request);
        User user = buildUser(request);
        userService.save(user);
        return generateTokens(user.getUid());
    }

    protected abstract void checkUniqueness(T request);

    protected abstract User buildUser(T request);

    protected void validate(T request) {
    }

    protected void checkCodeIfNeeded(T request) {
    }

    protected void doRegister(T request) {
    }

    protected Tokens generateTokens(Long uid) {
        return CommonUtils.createAccessTokenAndRefreshToken(uid);
    }

    protected void checkUnique(SFunction<User, ?> column, Object value) {
        if (!ObjectUtils.isEmpty(userService.lambdaQuery().eq(column, value).one())) {
            throw new BusinessException(ResultCode.USER_EXIST);
        }
    }

    protected User.UserBuilder createBaseBuilder() {
        return User.builder()
                .uid(idUtils.getNextId(IdBizEnum.USER.getBizType()))
                .registerTime(LocalDateTime.now())
                .status(1)
                .deleted(false);
    }

    protected String generateRandomNickname() {
        return RandomUtils.generateRandomNickname();
    }

    protected String generateEncodedPassword(String rawPassword) {
        return passwordUtils.encodePassword(rawPassword);
    }

    protected String generateRandomPassword() {
        return passwordUtils.generateDefaultPassword();
    }
}
