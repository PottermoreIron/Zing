package com.pot.auth.service.strategy.impl;

import com.pot.auth.service.dto.request.login.LoginRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.enums.LoginType;

/**
 * @author: Pot
 * @created: 2025/10/20 23:02
 * @description: 登录策略接口
 */
public interface LoginStrategy<T extends LoginRequest> {
    /**
     * 获取登录类型
     *
     * @return LoginType
     */
    LoginType getLoginType();

    /**
     * 是否支持该登录类型
     *
     * @param type 登录类型
     * @return boolean
     */
    default boolean supports(LoginType type) {
        return type.equals(getLoginType());
    }

    /**
     * 执行登录
     *
     * @param request 登录请求
     * @return AuthResponse
     */
    AuthResponse login(T request);
}
