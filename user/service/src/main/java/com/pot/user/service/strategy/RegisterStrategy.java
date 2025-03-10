package com.pot.user.service.strategy;

import com.pot.user.service.enums.RegisterType;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/3/10 23:12
 * @description: 注册策略类
 */
public interface RegisterStrategy {
    /**
     * 返回策略类型
     *
     * @return boolean
     * @author pot
     * @description
     * @date 23:17 2025/3/10
     **/
    RegisterType getStrategy();

    /**
     * 校验参数
     *
     * @param params 参数
     * @return boolean
     * @author pot
     * @description
     * @date 23:18 2025/3/10
     **/
    boolean validate(Map<String, String> params);

    /**
     * 注册
     *
     * @param params 参数
     * @author pot
     * @description
     * @date 23:21 2025/3/10
     **/
    default void register(Map<String, String> params) {
        if (!validate(params)) {
            throw new IllegalArgumentException("参数校验失败");
        }
        doRegister(params);
    }

    /**
     * 核心注册逻辑
     *
     * @param params 参数
     * @author pot
     * @description
     * @date 23:21 2025/3/10
     **/
    void doRegister(Map<String, String> params);
}
