package com.pot.auth.application.strategy.factory;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.application.strategy.LoginStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves login strategies by login type.
 */
@Slf4j
@Component
public class LoginStrategyFactory {

    private final Map<LoginType, LoginStrategy> strategyMap = new ConcurrentHashMap<>();

    public LoginStrategyFactory(List<LoginStrategy> strategies) {
        log.info("[登录策略工厂] 开始初始化，共有 {} 个策略", strategies.size());
        for (LoginStrategy strategy : strategies) {
            LoginType loginType = strategy.getSupportedLoginType();
            strategyMap.put(loginType, strategy);
            log.info("[登录策略工厂] 注册策略: {} -> {}", loginType, strategy.getClass().getSimpleName());
        }
        log.info("[登录策略工厂] 初始化完成，已注册策略: {}", strategyMap.keySet());
    }

    public LoginStrategy getStrategy(LoginType loginType) {
        LoginStrategy strategy = strategyMap.get(loginType);
        if (strategy == null) {
            log.error("[登录策略工厂] 未找到登录策略: loginType={}", loginType);
            throw new DomainException(AuthResultCode.UNSUPPORTED_LOGIN_TYPE);
        }
        log.debug("[登录策略工厂] 找到登录策略: {} -> {}", loginType, strategy.getClass().getSimpleName());
        return strategy;
    }

    public boolean supports(LoginType loginType) {
        return strategyMap.containsKey(loginType);
    }
}
