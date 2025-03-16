package com.pot.user.service.strategy;

import com.pot.user.service.controller.request.RegisterRequest;
import com.pot.user.service.enums.RegisterType;

/**
 * @author: Pot
 * @created: 2025/3/10 23:12
 * @description: 注册策略类
 */
public interface RegisterStrategy {
    /**
     * @return RegisterType
     * @author pot
     * @description
     * @date 21:29 2025/3/16
     **/
    RegisterType getRegisterType();

    /**
     * 是否支持注册类型
     *
     * @param type 注册类型
     * @return boolean
     * @author pot
     * @description
     * @date 23:39 2025/3/11
     **/
    boolean supports(RegisterType type);

    /**
     * 校验参数
     *
     * @param request 请求参数
     * @author pot
     * @description
     * @date 23:18 2025/3/10
     **/
    void validate(RegisterRequest request);

    /**
     * 注册
     *
     * @param request 请求参数
     * @author pot
     * @description
     * @date 23:21 2025/3/10
     **/
    void register(RegisterRequest request);
}
