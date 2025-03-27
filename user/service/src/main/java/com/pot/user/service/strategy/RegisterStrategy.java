package com.pot.user.service.strategy;

import com.pot.user.service.controller.request.register.RegisterRequest;
import com.pot.user.service.controller.response.Tokens;
import com.pot.user.service.enums.LoginRegisterType;

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
    LoginRegisterType getRegisterType();

    /**
     * 是否支持注册类型
     *
     * @param type 注册类型
     * @return boolean
     * @author pot
     * @description
     * @date 23:39 2025/3/11
     **/
    boolean supports(LoginRegisterType type);

    /**
     * 注册
     *
     * @param request 请求参数
     * @return Tokens 返回两个Token
     * @author pot
     * @description
     * @date 23:21 2025/3/10
     **/
    Tokens register(RegisterRequest request);
}
