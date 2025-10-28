package com.pot.auth.service.strategy.factory;

import com.pot.auth.service.enums.LoginType;
import com.pot.auth.service.strategy.impl.LoginStrategy;
import com.pot.zing.framework.common.excption.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/10/20
 * @description: 登录策略工厂 - 负责根据登录类型选择对应的登录策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginStrategyFactory {

    private final List<LoginStrategy<?>> loginStrategies;

    private volatile Map<LoginType, LoginStrategy<?>> strategyMap;

    /**
     * 获取登录策略
     *
     * @param loginType 登录类型
     * @return 对应的登录策略
     */
    public LoginStrategy<?> getStrategy(LoginType loginType) {
        if (strategyMap == null) {
            synchronized (this) {
                if (strategyMap == null) {
                    strategyMap = loginStrategies.stream()
                            .collect(Collectors.toMap(
                                    LoginStrategy::getLoginType,
                                    Function.identity(),
                                    (existing, replacement) -> {
                                        log.warn("发现重复的登录策略: {}, 使用第一个", existing.getLoginType());
                                        return existing;
                                    }
                            ));
                    log.info("登录策略工厂初始化完成，共加载 {} 个策略: {}",
                            strategyMap.size(),
                            strategyMap.keySet());
                }
            }
        }

        LoginStrategy<?> strategy = strategyMap.get(loginType);
        if (strategy == null) {
            throw new BusinessException("不支持的登录方式: " + loginType.getDescription());
        }
        return strategy;
    }

    /**
     * 检查是否支持该登录类型
     *
     * @param loginType 登录类型
     * @return 是否支持
     */
    public boolean supports(LoginType loginType) {
        if (strategyMap == null) {
            getStrategy(loginType); // 触发初始化
        }
        return strategyMap.containsKey(loginType);
    }
}