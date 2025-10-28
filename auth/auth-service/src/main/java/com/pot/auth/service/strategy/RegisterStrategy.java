package com.pot.auth.service.strategy;

import com.pot.auth.service.dto.request.register.RegisterRequest;
import com.pot.auth.service.dto.response.RegisterResponse;
import com.pot.auth.service.enums.RegisterType;

/**
 * @author: Pot
 * @created: 2025/10/13 23:28
 * @description: 注册策略类
 */
public interface RegisterStrategy<T extends RegisterRequest> {
    /**
     * @return RegisterType
     * @author pot
     * @description 获取注册类型
     * @date 23:31 2025/10/13
     **/
    RegisterType getRegisterType();

    /**
     * @param type 注册类型
     * @return boolean
     * @author pot
     * @description 是否支持该注册类型
     * @date 23:32 2025/10/13
     **/
    default boolean supports(RegisterType type) {
        return type.equals(getRegisterType());
    }

    /**
     * @param request 注册请求
     * @return R<RegisterResponse>
     * @author pot
     * @description 注册
     * @date 23:32 2025/10/13
     **/
    RegisterResponse register(T request);
}
