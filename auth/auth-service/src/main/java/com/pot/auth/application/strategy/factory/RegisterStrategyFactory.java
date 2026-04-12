package com.pot.auth.application.strategy.factory;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.application.strategy.RegisterStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves register strategies by register type.
 */
@Slf4j
@Component
public class RegisterStrategyFactory {

    private final Map<RegisterType, RegisterStrategy> strategyMap = new ConcurrentHashMap<>();

    public RegisterStrategyFactory(List<RegisterStrategy> strategies) {
        log.info("[RegisterStrategyFactory] Initializing with {} strategies", strategies.size());
        for (RegisterStrategy strategy : strategies) {
            RegisterType registerType = strategy.getSupportedRegisterType();
            strategyMap.put(registerType, strategy);
            log.info("[RegisterStrategyFactory] Mapped strategy: {} -> {}", registerType, strategy.getClass().getSimpleName());
        }
        log.info("[RegisterStrategyFactory] Initialization complete, strategies: {}", strategyMap.keySet());
    }

    public RegisterStrategy getStrategy(RegisterType registerType) {
        RegisterStrategy strategy = strategyMap.get(registerType);
        if (strategy == null) {
            log.error("[RegisterStrategyFactory] No strategy found — registerType={}", registerType);
            throw new DomainException(AuthResultCode.UNSUPPORTED_REGISTER_TYPE);
        }
        log.debug("[RegisterStrategyFactory] Strategy resolved: {} -> {}", registerType, strategy.getClass().getSimpleName());
        return strategy;
    }

    public boolean supports(RegisterType registerType) {
        return strategyMap.containsKey(registerType);
    }
}
