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
        log.info("[LoginStrategyFactory] Initializing with {} strategies", strategies.size());
        for (LoginStrategy strategy : strategies) {
            LoginType loginType = strategy.getSupportedLoginType();
            strategyMap.put(loginType, strategy);
            log.info("[LoginStrategyFactory] Mapped strategy: {} -> {}", loginType, strategy.getClass().getSimpleName());
        }
        log.info("[LoginStrategyFactory] Initialization complete, strategies: {}", strategyMap.keySet());
    }

    public LoginStrategy getStrategy(LoginType loginType) {
        LoginStrategy strategy = strategyMap.get(loginType);
        if (strategy == null) {
            log.error("[LoginStrategyFactory] No strategy found — loginType={}", loginType);
            throw new DomainException(AuthResultCode.UNSUPPORTED_LOGIN_TYPE);
        }
        log.debug("[LoginStrategyFactory] Strategy resolved: {} -> {}", loginType, strategy.getClass().getSimpleName());
        return strategy;
    }

    public boolean supports(LoginType loginType) {
        return strategyMap.containsKey(loginType);
    }
}
