package com.pot.auth.service.strategy.factory;

import com.pot.auth.service.dto.request.signinorregister.SignInOrRegisterRequest;
import com.pot.auth.service.enums.SignInOrRegisterType;
import com.pot.auth.service.strategy.SignInOrRegisterStrategy;
import com.pot.zing.framework.common.excption.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 一键登录/注册策略工厂
 *
 * <p>职责：</p>
 * <ul>
 *   <li>管理所有一键登录/注册策略实例</li>
 *   <li>根据认证类型自动路由到对应策略</li>
 *   <li>提供类型安全的策略获取方法</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignInOrRegisterStrategyFactory {

    /**
     * Spring 自动注入所有 SignInOrRegisterStrategy 的实现类
     */
    private final List<SignInOrRegisterStrategy<? extends SignInOrRegisterRequest>> strategies;

    /**
     * 根据类型获取对应的策略
     *
     * @param type 认证类型
     * @param <T>  请求类型
     * @return 对应的策略实例
     * @throws BusinessException 如果找不到对应的策略
     */
    @SuppressWarnings("unchecked")
    public <T extends SignInOrRegisterRequest> SignInOrRegisterStrategy<T> getStrategy(
            SignInOrRegisterType type) {

        log.debug("查找一键登录/注册策略: type={}", type);

        return (SignInOrRegisterStrategy<T>) strategies.stream()
                .filter(strategy -> strategy.supports(type))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("未找到对应的一键登录/注册策略: type={}", type);
                    return new BusinessException("不支持的一键登录类型: " + type);
                });
    }

    /**
     * 检查是否支持指定的认证类型
     *
     * @param type 认证类型
     * @return true-支持，false-不支持
     */
    public boolean isSupported(SignInOrRegisterType type) {
        return strategies.stream()
                .anyMatch(strategy -> strategy.supports(type));
    }
}

