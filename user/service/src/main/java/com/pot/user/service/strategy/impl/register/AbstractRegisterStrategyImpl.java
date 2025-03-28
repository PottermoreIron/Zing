package com.pot.user.service.strategy.impl.register;

import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.controller.response.Tokens;
import com.pot.user.service.strategy.RegisterStrategy;
import com.pot.user.service.utils.JwtUtils;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import com.sankuai.inf.leaf.service.SegmentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/3/10 23:22
 * @description: 抽象注册策略类
 */
@Service
public abstract class AbstractRegisterStrategyImpl implements RegisterStrategy {
    private final String BIZ_TYPE = "user";

    @Resource
    private SegmentService segmentService;

    @Override
    public Tokens register(RegisterRequest request) {
        // 校验参数
        validate(request);
        // 校验唯一性
        checkUniqueness(request);
        // 校验验证码
        checkCodeIfNeeded(request);
        // 注册
        Long uid = doRegister(request);
        return getTokens(uid);
    }

    protected Tokens getTokens(Long uid) {
        return Tokens.builder()
                .accessToken(JwtUtils.createAccessToken(uid))
                .refreshToken(JwtUtils.createRefreshToken(uid))
                .build();
    }

    protected Long getNextId() {
        try {
            Result result = segmentService.getId(BIZ_TYPE);
            if (result.getStatus().equals(Status.EXCEPTION)) {
                throw new RuntimeException("获取分布式ID异常");
            }
            return result.getId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void validate(RegisterRequest request);

    protected abstract void checkUniqueness(RegisterRequest request);

    protected abstract void checkCodeIfNeeded(RegisterRequest request);

    protected abstract Long doRegister(RegisterRequest request);


}
