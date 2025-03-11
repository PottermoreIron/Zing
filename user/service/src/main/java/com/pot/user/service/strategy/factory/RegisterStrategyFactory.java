package com.pot.user.service.strategy.factory;

import com.pot.user.service.enums.RegisterType;
import com.pot.user.service.strategy.RegisterStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/3/11 23:47
 * @description: 注册策略工厂实现类
 */
@Component
@RequiredArgsConstructor
public class RegisterStrategyFactory {
    private final List<RegisterStrategy> strategies;
    private Map<RegisterType, RegisterStrategy> strategyMap = new HashMap<>();
    private Map<Integer, RegisterStrategy> strategyCodeMap = new HashMap<>();

    @PostConstruct
    void init() {
        // 双重校验确保每个注册类型都有唯一策略
        for (RegisterType type : RegisterType.values()) {
            List<RegisterStrategy> supportedStrategies = strategies.stream()
                    .filter(strategy -> strategy.supports(type))
                    .toList();
            if (supportedStrategies.isEmpty()) {
                throw new IllegalStateException("未找到注册类型[" + type + "]对应的策略实现");
            }

            if (supportedStrategies.size() > 1) {
                String conflictStrategies = supportedStrategies.stream()
                        .map(s -> s.getClass().getSimpleName())
                        .collect(Collectors.joining(", "));
                throw new IllegalStateException("注册类型[" + type + "]存在多个策略冲突: " + conflictStrategies);
            }
            strategyMap.put(type, supportedStrategies.getFirst());
            strategyCodeMap.put(type.getCode(), supportedStrategies.getFirst());
        }
    }

    public RegisterStrategy getStrategy(RegisterType type) {
        return Optional.ofNullable(strategyMap.get(type))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的注册类型: " + type));
    }

    public RegisterStrategy getStrategyByCode(int code) {
        return Optional.ofNullable(strategyCodeMap.get(code))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的注册类型: " + code));
    }
}
