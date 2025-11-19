package com.pot.auth.domain.strategy.factory;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.strategy.LoginStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录策略工厂
 *
 * <p>负责根据登录类型自动选择对应的登录策略
 * <p>通过Spring自动注入所有LoginStrategy实现类，避免硬编码
 *
 * @author yecao
 * @since 2025-11-19
 */
@Slf4j
@Component
public class LoginStrategyFactory {

    private final Map<LoginType, LoginStrategy> strategyMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，Spring自动注入所有LoginStrategy实现类
     *
     * @param strategies 所有登录策略实现类
     */
    public LoginStrategyFactory(List<LoginStrategy> strategies) {
        log.info("[登录策略工厂] 开始初始化，共有 {} 个策略", strategies.size());

        for (LoginStrategy strategy : strategies) {
            // 遍历所有登录类型枚举值，找到该策略支持的类型
            for (LoginType loginType : LoginType.values()) {
                registerStrategy(strategy, loginType);
            }
        }

        log.info("[登录策略工厂] 初始化完成，已注册策略: {}", strategyMap.keySet());
    }

    /**
     * 注册策略到Map
     */
    private void registerStrategy(LoginStrategy strategy, LoginType loginType) {
        if (strategy.supports(loginType)) {
            strategyMap.put(loginType, strategy);
            log.info("[登录策略工厂] 注册策略: {} -> {}", loginType, strategy.getClass().getSimpleName());
        }
    }

    /**
     * 根据登录类型获取对应的策略
     *
     * @param loginType 登录类型
     * @return 登录策略
     * @throws DomainException 当找不到对应策略时
     */
    public LoginStrategy getStrategy(LoginType loginType) {
        LoginStrategy strategy = strategyMap.get(loginType);

        if (strategy == null) {
            log.error("[登录策略工厂] 未找到登录策略: loginType={}", loginType);
            throw new DomainException(AuthResultCode.UNSUPPORTED_LOGIN_TYPE);
        }

        log.debug("[登录策略工厂] 找到登录策略: {} -> {}",
                loginType, strategy.getClass().getSimpleName());
        return strategy;
    }

    /**
     * 检查是否支持指定的登录类型
     *
     * @param loginType 登录类型
     * @return true if支持, false otherwise
     */
    public boolean supports(LoginType loginType) {
        return strategyMap.containsKey(loginType);
    }
}

