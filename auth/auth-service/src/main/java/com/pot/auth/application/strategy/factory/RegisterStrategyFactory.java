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
 * 注册策略工厂
 *
 * <p>
 * 负责根据注册类型自动选择对应的注册策略。
 * 通过Spring自动注入所有 {@link RegisterStrategy} 实现类，避免硬编码。
 *
 * @author pot
 * @since 2025-11-19
 */
@Slf4j
@Component
public class RegisterStrategyFactory {

    private final Map<RegisterType, RegisterStrategy> strategyMap = new ConcurrentHashMap<>();

    public RegisterStrategyFactory(List<RegisterStrategy> strategies) {
        log.info("[注册策略工厂] 开始初始化，共有 {} 个策略", strategies.size());
        for (RegisterStrategy strategy : strategies) {
            RegisterType registerType = strategy.getSupportedRegisterType();
            strategyMap.put(registerType, strategy);
            log.info("[注册策略工厂] 注册策略: {} -> {}", registerType, strategy.getClass().getSimpleName());
        }
        log.info("[注册策略工厂] 初始化完成，已注册策略: {}", strategyMap.keySet());
    }

    public RegisterStrategy getStrategy(RegisterType registerType) {
        RegisterStrategy strategy = strategyMap.get(registerType);
        if (strategy == null) {
            log.error("[注册策略工厂] 未找到注册策略: registerType={}", registerType);
            throw new DomainException(AuthResultCode.UNSUPPORTED_REGISTER_TYPE);
        }
        log.debug("[注册策略工厂] 找到注册策略: {} -> {}", registerType, strategy.getClass().getSimpleName());
        return strategy;
    }

    public boolean supports(RegisterType registerType) {
        return strategyMap.containsKey(registerType);
    }
}
