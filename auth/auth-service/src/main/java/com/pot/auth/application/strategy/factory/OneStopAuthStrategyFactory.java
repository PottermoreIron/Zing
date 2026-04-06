package com.pot.auth.application.strategy.factory;

import com.pot.auth.application.strategy.OneStopAuthStrategy;
import com.pot.auth.domain.shared.enums.AuthType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves one-stop auth strategies by auth type.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OneStopAuthStrategyFactory {

    private final List<OneStopAuthStrategy> strategies;
    private final Map<AuthType, OneStopAuthStrategy> strategyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[一键认证策略工厂] 开始初始化...");
        for (OneStopAuthStrategy strategy : strategies) {
            AuthType authType = strategy.getSupportedAuthType();
            strategyCache.put(authType, strategy);
            log.info("[一键认证策略工厂] 注册策略: authType={}, strategy={}", authType, strategy.getClass().getSimpleName());
        }
        log.info("[一键认证策略工厂] 初始化完成，共注册 {} 个策略", strategyCache.size());
    }

    public OneStopAuthStrategy getStrategy(AuthType authType) {
        OneStopAuthStrategy strategy = strategyCache.get(authType);
        if (strategy == null) {
            log.error("[一键认证策略工厂] 未找到策略: authType={}", authType);
            throw new IllegalArgumentException("不支持的认证类型: " + authType);
        }
        return strategy;
    }

    public boolean supports(AuthType authType) {
        return strategyCache.containsKey(authType);
    }

    public List<AuthType> getSupportedAuthTypes() {
        return strategyCache.keySet().stream().toList();
    }
}
