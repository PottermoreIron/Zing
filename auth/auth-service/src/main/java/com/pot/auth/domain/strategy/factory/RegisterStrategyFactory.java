package com.pot.auth.domain.strategy.factory;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.strategy.RegisterStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册策略工厂
 *
 * <p>负责根据注册类型自动选择对应的注册策略
 * <p>通过Spring自动注入所有RegisterStrategy实现类，避免硬编码
 *
 * @author pot
 * @since 2025-11-19
 */
@Slf4j
@Component
public class RegisterStrategyFactory {

    private final Map<RegisterType, RegisterStrategy<?>> strategyMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，Spring自动注入所有RegisterStrategy实现类
     *
     * @param strategies 所有注册策略实现类
     */
    public RegisterStrategyFactory(List<RegisterStrategy<?>> strategies) {
        log.info("[注册策略工厂] 开始初始化，共有 {} 个策略", strategies.size());

        for (RegisterStrategy<?> strategy : strategies) {
            // 遍历所有注册类型枚举值，找到该策略支持的类型
            for (RegisterType registerType : RegisterType.values()) {
                registerStrategy(strategy, registerType);
            }
        }

        log.info("[注册策略工厂] 初始化完成，已注册策略: {}", strategyMap.keySet());
    }

    /**
     * 注册策略到Map
     */
    private void registerStrategy(RegisterStrategy<?> strategy, RegisterType registerType) {
        if (strategy.supports(registerType)) {
            strategyMap.put(registerType, strategy);
            log.info("[注册策略工厂] 注册策略: {} -> {}", registerType, strategy.getClass().getSimpleName());
        }
    }

    /**
     * 根据注册类型获取对应的策略
     *
     * @param registerType 注册类型
     * @return 注册策略
     * @throws DomainException 当找不到对应策略时
     */
    public RegisterStrategy<?> getStrategy(RegisterType registerType) {
        RegisterStrategy<?> strategy = strategyMap.get(registerType);

        if (strategy == null) {
            log.error("[注册策略工厂] 未找到注册策略: registerType={}", registerType);
            throw new DomainException(AuthResultCode.UNSUPPORTED_REGISTER_TYPE);
        }

        log.debug("[注册策略工厂] 找到注册策略: {} -> {}",
                registerType, strategy.getClass().getSimpleName());
        return strategy;
    }

    /**
     * 检查是否支持指定的注册类型
     *
     * @param registerType 注册类型
     * @return true if支持, false otherwise
     */
    public boolean supports(RegisterType registerType) {
        return strategyMap.containsKey(registerType);
    }
}

