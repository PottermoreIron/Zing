package com.pot.user.service.strategy.factory;

import com.pot.user.service.enums.VerificationCodeType;
import com.pot.user.service.strategy.VerificationCodeStrategy;
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
 * @created: 2025/3/27 23:57
 * @description: 验证码策略工厂
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeStrategyFactory {
    private final List<VerificationCodeStrategy> strategies;
    private final Map<VerificationCodeType, VerificationCodeStrategy> strategyMap = new HashMap<>();
    private final Map<Integer, VerificationCodeStrategy> strategyCodeMap = new HashMap<>();

    @PostConstruct
    void init() {
        strategies.forEach(strategy -> {
            VerificationCodeType type = strategy.getVerificationCodeType();
            Integer code = type.getCode();
            strategyMap.put(type, strategy);
            strategyCodeMap.put(code, strategy);
        });
    }

    public VerificationCodeStrategy getStrategy(VerificationCodeType type) {
        return Optional.ofNullable(strategyMap.get(type))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的发送类型: " + type));
    }

    public VerificationCodeStrategy getStrategyByCode(int code) {
        return Optional.ofNullable(strategyCodeMap.get(code))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的发送类型: " + code));
    }
}
