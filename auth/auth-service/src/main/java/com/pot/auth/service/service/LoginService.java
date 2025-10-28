package com.pot.auth.service.service;

import com.pot.auth.service.dto.request.login.LoginRequest;
import com.pot.auth.service.dto.response.AuthResponse;

/**
 * @author: Pot
 * @created: 2025/10/20
 * @description: 登录服务接口
 */
public interface LoginService {
    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证响应
     */
    AuthResponse login(LoginRequest request);

    /**
     * 退出登录
     *
     * @param userId 用户ID
     */
    void logout(Long userId);

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新令牌
     * @return 新的认证响应
     */
    AuthResponse refreshToken(String refreshToken);
}

