package com.pot.auth.service.strategy;

import com.pot.auth.service.dto.request.signinorregister.SignInOrRegisterRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.enums.SignInOrRegisterType;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 一键登录/注册策略接口
 *
 * <p>核心流程：</p>
 * <ol>
 *   <li>验证凭证（验证码/OAuth2 Token）</li>
 *   <li>检查用户是否存在</li>
 *   <li>存在 → 执行登录流程</li>
 *   <li>不存在 → 执行注册流程 + 自动登录</li>
 * </ol>
 */
public interface SignInOrRegisterStrategy<T extends SignInOrRegisterRequest> {

    /**
     * 获取支持的认证类型
     *
     * @return SignInOrRegisterType
     */
    SignInOrRegisterType getType();

    /**
     * 是否支持该认证类型
     *
     * @param type 认证类型
     * @return boolean
     */
    default boolean supports(SignInOrRegisterType type) {
        return type.equals(getType());
    }

    /**
     * 执行一键登录/注册
     *
     * @param request 请求对象
     * @return AuthResponse 认证响应（包含Token和用户信息）
     */
    AuthResponse signInOrRegister(T request);
}

