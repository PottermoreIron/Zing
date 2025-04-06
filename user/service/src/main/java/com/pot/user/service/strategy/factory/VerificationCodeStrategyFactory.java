package com.pot.user.service.strategy.factory;

import com.pot.user.service.enums.SendCodeChannelEnum;
import com.pot.user.service.strategy.SendCodeStrategy;
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
 * @created: 2025/3/27 23:57
 * @description: 验证码策略工厂
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeStrategyFactory {
    private final List<SendCodeStrategy> strategies;
    private final Map<SendCodeChannelEnum, SendCodeStrategy> strategyMap = new ConcurrentHashMap<>();
    private final Map<Integer, SendCodeStrategy> strategyCodeMap = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        strategies.forEach(strategy -> {
            SendCodeChannelEnum type = strategy.getVerificationCodeType();
            Integer code = type.getCode();
            strategyMap.put(type, strategy);
            strategyCodeMap.put(code, strategy);
        });
    }

    public SendCodeStrategy getStrategy(SendCodeChannelEnum type) {
        return Optional.ofNullable(strategyMap.get(type))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的发送类型: " + type));
    }

    public SendCodeStrategy getStrategyByCode(int code) {
        return Optional.ofNullable(strategyCodeMap.get(code))
                .orElseThrow(() -> new UnsupportedOperationException("不支持的发送类型: " + code));
    }
}
