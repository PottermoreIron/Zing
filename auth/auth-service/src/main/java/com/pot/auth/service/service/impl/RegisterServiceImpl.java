package com.pot.auth.service.service.impl;

import com.pot.auth.service.dto.request.register.RegisterRequest;
import com.pot.auth.service.dto.response.RegisterResponse;
import com.pot.auth.service.service.RegisterService;
import com.pot.auth.service.strategy.RegisterStrategy;
import com.pot.auth.service.strategy.factory.RegisterStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/10/19 21:42
 * @description: 注册服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final RegisterStrategyFactory strategyFactory;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        log.info("收到注册请求: type={}", request.getType());

        // 通过工厂获取对应的策略
        RegisterStrategy<RegisterRequest> strategy = strategyFactory.getStrategy(request.getType());

        // 执行注册
        RegisterResponse response = strategy.register(request);

        log.info("注册完成: type={}", request.getType());
        return response;
    }
}
