package com.pot.user.service.strategy.impl;

import com.pot.user.service.controller.request.RegisterRequest;
import com.pot.user.service.strategy.RegisterStrategy;

/**
 * @author: Pot
 * @created: 2025/3/10 23:22
 * @description: 抽象注册策略类
 */
public abstract class AbstractRegisterStrategyImpl implements RegisterStrategy {
    @Override
    public void register(RegisterRequest request) {
        // 校验参数
        validate(request);
        // 校验唯一性
        checkUniqueness(request);
        // 校验验证码
        checkCodeIfNeeded(request);
        // 注册
        doRegister(request);
    }

    protected abstract void validate(RegisterRequest request);

    protected abstract void checkUniqueness(RegisterRequest request);

    protected abstract void checkCodeIfNeeded(RegisterRequest request);

    protected abstract void doRegister(RegisterRequest request);
}
