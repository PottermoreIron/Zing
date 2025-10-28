package com.pot.auth.service.service.impl;

import com.pot.auth.service.dto.request.signinorregister.SignInOrRegisterRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.service.SignInOrRegisterService;
import com.pot.auth.service.strategy.SignInOrRegisterStrategy;
import com.pot.auth.service.strategy.factory.SignInOrRegisterStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 一键登录/注册服务实现
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>接收一键登录/注册请求</li>
 *   <li>通过工厂获取对应的策略</li>
 *   <li>委托策略执行具体的业务逻辑</li>
 *   <li>返回统一的认证响应</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignInOrRegisterServiceImpl implements SignInOrRegisterService {

    private final SignInOrRegisterStrategyFactory strategyFactory;

    @Override
    public AuthResponse signInOrRegister(SignInOrRegisterRequest request) {
        log.info("处理一键登录/注册请求: type={}", request.getType());

        // 1. 根据类型获取对应的策略
        SignInOrRegisterStrategy<SignInOrRegisterRequest> strategy =
                strategyFactory.getStrategy(request.getType());

        log.debug("找到对应的策略: strategyClass={}", strategy.getClass().getSimpleName());

        // 2. 执行策略
        AuthResponse response = strategy.signInOrRegister(request);

        log.info("一键登录/注册处理完成: type={}, memberId={}",
                request.getType(),
                response.getUserInfo() != null ? response.getUserInfo().getMemberId() : "unknown");

        return response;
    }
}


