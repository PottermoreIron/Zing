package com.pot.user.service.strategy.impl.register;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.pot.common.enums.ResultCode;
import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.controller.response.Tokens;
import com.pot.user.service.entity.User;
import com.pot.user.service.exception.BusinessException;
import com.pot.user.service.service.UserService;
import com.pot.user.service.strategy.RegisterStrategy;
import com.pot.user.service.strategy.factory.VerificationCodeStrategyFactory;
import com.pot.user.service.utils.JwtUtils;
import com.pot.user.service.utils.PasswordUtils;
import com.pot.user.service.utils.RandomStringGenerator;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import com.sankuai.inf.leaf.service.SegmentService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    
    private final String BIZ_TYPE = "user";
    protected final UserService userService;
    protected final SegmentService segmentService;
    protected final VerificationCodeStrategyFactory verificationCodeStrategyFactory;
    protected final PasswordEncoder passwordEncoder;

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
        return JwtUtils.createAccessTokenAndRefreshToken(uid);
    }

    protected Long getNextId() {
        try {
            Result result = segmentService.getId(BIZ_TYPE);
            if (result.getStatus().equals(Status.EXCEPTION)) {
                throw new BusinessException(ResultCode.GET_ID_EXCEPTION);
            }
            return result.getId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void checkUnique(SFunction<User, ?> column, Object value) {
        if (!ObjectUtils.isEmpty(userService.lambdaQuery().eq(column, value).one())) {
            throw new BusinessException(ResultCode.USER_EXIST);
        }
    }

    protected User.UserBuilder createBaseBuilder() {
        return User.builder()
                .uid(getNextId())
                .registerTime(LocalDateTime.now())
                .status(0)
                .deleted(false);
    }

    protected String generateRandomNickname() {
        return RandomStringGenerator.generateRandomNickname();
    }

    protected String generateEncodedPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    protected String generateRandomPassword() {
        return PasswordUtils.generateDefaultPassword();
    }
}
