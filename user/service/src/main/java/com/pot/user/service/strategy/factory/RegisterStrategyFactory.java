package com.pot.user.service.strategy.factory;

import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.enums.LoginRegisterType;
import com.pot.user.service.strategy.RegisterStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Pot
 * @created: 2025/3/11 23:47
 * @description: 注册策略工厂实现类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterStrategyFactory {
    private final List<RegisterStrategy<?>> strategies;
    private final Map<LoginRegisterType, RegisterStrategy<?>> strategyMap = new ConcurrentHashMap<>();
    private final Map<Integer, RegisterStrategy<?>> strategyCodeMap = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        strategies.forEach(strategy -> {
            LoginRegisterType loginRegisterType = strategy.getRegisterType();
            Integer code = loginRegisterType.getCode();
            strategyMap.put(loginRegisterType, strategy);
            strategyCodeMap.put(code, strategy);
        });
    }

    public <T extends RegisterRequest> RegisterStrategy<T> getStrategy(LoginRegisterType type) {
        return Optional.ofNullable((RegisterStrategy<T>) strategyMap.get(type))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的注册类型: " + type));
    }

    public <T extends RegisterRequest> RegisterStrategy<T> getStrategyByCode(int code) {
        return Optional.ofNullable((RegisterStrategy<T>) strategyCodeMap.get(code))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的注册类型: " + code));
    }
}
