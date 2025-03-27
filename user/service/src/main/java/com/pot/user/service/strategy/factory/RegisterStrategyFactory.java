package com.pot.user.service.strategy.factory;

import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.strategy.RegisterStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author: Pot
 * @created: 2025/3/11 23:47
 * @description: 注册策略工厂实现类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterStrategyFactory {
    private final List<RegisterStrategy> strategies;
    private final Map<LoginRegisterType, RegisterStrategy> strategyMap = new HashMap<>();
    private final Map<Integer, RegisterStrategy> strategyCodeMap = new HashMap<>();

    @PostConstruct
    void init() {
        strategies.forEach(strategy -> {
            LoginRegisterType loginRegisterType = strategy.getRegisterType();
            Integer code = loginRegisterType.getCode();
            strategyMap.put(loginRegisterType, strategy);
            strategyCodeMap.put(code, strategy);
        });
    }

    public RegisterStrategy getStrategy(LoginRegisterType type) {
        return Optional.ofNullable(strategyMap.get(type))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的注册类型: " + type));
    }

    public RegisterStrategy getStrategyByCode(int code) {
        return Optional.ofNullable(strategyCodeMap.get(code))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的注册类型: " + code));
    }
}
