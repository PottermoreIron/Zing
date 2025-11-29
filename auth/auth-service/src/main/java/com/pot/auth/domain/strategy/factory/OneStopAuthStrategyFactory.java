package com.pot.auth.domain.strategy.factory;

import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.strategy.OneStopAuthStrategy;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一键认证策略工厂
 *
 * <p>
 * 负责管理和分发一键认证策略实例
 *
 * <p>
 * <strong>设计模式：</strong>
 * <ul>
 * <li>工厂模式 - 统一创建策略对象</li>
 * <li>策略模式 - 封装不同的认证算法</li>
 * <li>依赖注入 - Spring自动注入所有策略实现</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OneStopAuthStrategyFactory {

    /**
     * Spring自动注入所有OneStopAuthStrategy实现类
     */
    private final List<OneStopAuthStrategy<? extends OneStopAuthRequest>> strategies;

    /**
     * 策略缓存（AuthType -> Strategy）
     */
    private final Map<AuthType, OneStopAuthStrategy<? extends OneStopAuthRequest>> strategyCache = new ConcurrentHashMap<>();

    /**
     * 初始化策略缓存
     */
    @PostConstruct
    public void init() {
        log.info("[策略工厂] 开始初始化一键认证策略...");

        for (OneStopAuthStrategy<? extends OneStopAuthRequest> strategy : strategies) {
            AuthType authType = strategy.getSupportedAuthType();
            strategyCache.put(authType, strategy);
            log.info("[策略工厂] 注册策略: authType={}, strategy={}",
                    authType, strategy.getClass().getSimpleName());
        }

        log.info("[策略工厂] 一键认证策略初始化完成，共注册 {} 个策略", strategyCache.size());
    }

    /**
     * 根据认证类型获取对应的策略
     *
     * @param authType 认证类型
     * @return 对应的策略实例
     * @throws IllegalArgumentException 如果找不到对应的策略
     */
    public OneStopAuthStrategy<? extends OneStopAuthRequest> getStrategy(AuthType authType) {
        OneStopAuthStrategy<? extends OneStopAuthRequest> strategy = strategyCache.get(authType);

        if (strategy == null) {
            log.error("[策略工厂] 未找到策略: authType={}", authType);
            throw new IllegalArgumentException("不支持的认证类型: " + authType);
        }

        return strategy;
    }

    /**
     * 判断是否支持某种认证类型
     *
     * @param authType 认证类型
     * @return true if 支持, false otherwise
     */
    public boolean supports(AuthType authType) {
        return strategyCache.containsKey(authType);
    }

    /**
     * 获取所有支持的认证类型
     *
     * @return 认证类型列表
     */
    public List<AuthType> getSupportedAuthTypes() {
        return strategyCache.keySet().stream().toList();
    }
}
