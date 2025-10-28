package com.pot.auth.service.strategy.factory;

import com.pot.auth.service.enums.RegisterType;
import com.pot.auth.service.strategy.RegisterStrategy;
import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.excption.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Pot
 * @created: 2025/10/19 21:40
 * @description: 注册策略工厂
 */
@Slf4j
@Component
public class RegisterStrategyFactory {

    private final Map<RegisterType, RegisterStrategy<?>> strategyMap = new ConcurrentHashMap<>();

    /**
     * 构造函数注入所有策略实现
     */
    public RegisterStrategyFactory(List<RegisterStrategy<?>> strategies) {
        strategies.forEach(strategy -> {
            RegisterType type = strategy.getRegisterType();
            if (strategyMap.containsKey(type)) {
                log.warn("重复注册策略: type={}, strategy={}", type, strategy.getClass().getName());
            }
            strategyMap.put(type, strategy);
            log.info("注册策略加载成功: type={}, strategy={}", type, strategy.getClass().getSimpleName());
        });
    }

    /**
     * 根据注册类型获取对应的策略
     */
    @SuppressWarnings("unchecked")
    public <T extends RegisterStrategy<?>> T getStrategy(RegisterType type) {
        RegisterStrategy<?> strategy = strategyMap.get(type);
        if (strategy == null) {
            log.error("不支持的注册类型: type={}", type);
            throw new BusinessException(ResultCode.REGISTER_EXCEPTION, "不支持的注册类型: " + type);
        }
        return (T) strategy;
    }

    /**
     * 检查是否支持该注册类型
     */
    public boolean supports(RegisterType type) {
        return strategyMap.containsKey(type);
    }
}
