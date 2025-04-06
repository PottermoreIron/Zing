package com.pot.user.service.strategy.factory;

import com.pot.user.service.enums.OAuth2Enum;
import com.pot.user.service.strategy.OAuth2LoginStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Pot
 * @created: 2025/4/6 21:11
 * @description: OAuth2登录策略工厂类
 */
@Service
@RequiredArgsConstructor
public class OAuth2LoginStrategyFactory {
    private final List<OAuth2LoginStrategy> strategies;
    private final Map<OAuth2Enum, OAuth2LoginStrategy> strategyMap = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        strategies.forEach(strategy -> {
            OAuth2Enum oauth2Enum = strategy.getType();
            strategyMap.put(oauth2Enum, strategy);
        });
    }

    public OAuth2LoginStrategy getStrategy(OAuth2Enum type) {
        return strategyMap.getOrDefault(type, null);
    }

    public OAuth2LoginStrategy getStrategy(String name) {
        return strategyMap.getOrDefault(OAuth2Enum.getByName(name), null);
    }
}
