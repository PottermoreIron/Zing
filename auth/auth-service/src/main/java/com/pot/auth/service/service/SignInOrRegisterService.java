package com.pot.auth.service.service;

import com.pot.auth.service.dto.request.signinorregister.SignInOrRegisterRequest;
import com.pot.auth.service.dto.response.AuthResponse;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 一键登录/注册服务接口
 */
public interface SignInOrRegisterService {

    /**
     * 一键登录/注册
     *
     * <p>业务逻辑：</p>
     * <ul>
     *   <li>已注册用户：直接登录</li>
     *   <li>未注册用户：自动注册后登录</li>
     * </ul>
     *
     * @param request 一键登录/注册请求
     * @return AuthResponse 认证响应（包含Token和用户信息）
     */
    AuthResponse signInOrRegister(SignInOrRegisterRequest request);
}