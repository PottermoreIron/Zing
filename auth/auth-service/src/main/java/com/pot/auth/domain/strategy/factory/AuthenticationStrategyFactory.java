package com.pot.auth.domain.strategy.factory;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.strategy.AuthenticationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一认证策略工厂
 *
 * <p>负责管理OAuth2、WeChat等一体化认证策略
 * <p>这类策略的特点是注册和登录合二为一，无需用户显式选择
 *
 * @author yecao
 * @since 2025-11-19
 */
@Slf4j
@Component
public class AuthenticationStrategyFactory {

    private final Map<String, AuthenticationStrategy> strategyMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，Spring自动注入所有AuthenticationStrategy实现类
     *
     * @param strategies 所有认证策略实现类
     */
    public AuthenticationStrategyFactory(List<AuthenticationStrategy> strategies) {
        log.info("[认证策略工厂] 开始初始化，共有 {} 个策略", strategies.size());

        for (AuthenticationStrategy strategy : strategies) {
            // 尝试所有可能的认证类型，找到该策略支持的类型
            registerStrategy(strategy, "OAUTH2");
            registerStrategy(strategy, "WECHAT");
        }

        log.info("[认证策略工厂] 初始化完成，已注册策略: {}", strategyMap.keySet());
    }

    /**
     * 注册策略到Map
     */
    private void registerStrategy(AuthenticationStrategy strategy, String authenticationType) {
        if (strategy.supports(authenticationType)) {
            strategyMap.put(authenticationType, strategy);
            log.info("[认证策略工厂] 注册策略: {} -> {}",
                    authenticationType, strategy.getClass().getSimpleName());
        }
    }

    /**
     * 根据认证类型获取对应的策略
     *
     * @param authenticationType 认证类型（OAUTH2, WECHAT等）
     * @return 认证策略
     * @throws DomainException 当找不到对应策略时
     */
    public AuthenticationStrategy getStrategy(String authenticationType) {
        AuthenticationStrategy strategy = strategyMap.get(authenticationType);

        if (strategy == null) {
            log.error("[认证策略工厂] 未找到认证策略: authenticationType={}", authenticationType);
            throw new DomainException(AuthResultCode.UNSUPPORTED_AUTHENTICATION_TYPE);
        }

        log.debug("[认证策略工厂] 找到认证策略: {} -> {}",
                authenticationType, strategy.getClass().getSimpleName());
        return strategy;
    }

    /**
     * 检查是否支持指定的认证类型
     *
     * @param authenticationType 认证类型
     * @return true if支持, false otherwise
     */
    public boolean supports(String authenticationType) {
        return strategyMap.containsKey(authenticationType);
    }
}

