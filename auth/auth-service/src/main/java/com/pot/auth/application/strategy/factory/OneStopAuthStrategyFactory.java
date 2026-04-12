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
        log.info("[OneStopAuthStrategyFactory] Initializing...");
        for (OneStopAuthStrategy strategy : strategies) {
            AuthType authType = strategy.getSupportedAuthType();
            strategyCache.put(authType, strategy);
            log.info("[OneStopAuthStrategyFactory] Mapped strategy: authType={}, strategy={}", authType, strategy.getClass().getSimpleName());
        }
        log.info("[OneStopAuthStrategyFactory] Initialization complete, {} strategies registered", strategyCache.size());
    }

    public OneStopAuthStrategy getStrategy(AuthType authType) {
        OneStopAuthStrategy strategy = strategyCache.get(authType);
        if (strategy == null) {
            log.error("[OneStopAuthStrategyFactory] No strategy found — authType={}", authType);
            throw new IllegalArgumentException("Unsupported authentication type: " + authType);
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
