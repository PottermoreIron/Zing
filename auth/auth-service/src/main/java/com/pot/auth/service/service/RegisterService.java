package com.pot.auth.service.service;

import com.pot.auth.service.dto.request.register.RegisterRequest;
import com.pot.auth.service.dto.response.RegisterResponse;

/**
 * @author: Pot
 * @created: 2025/10/19 21:41
 * @description: 注册服务接口
 */
public interface RegisterService {
    /**
     * 用户注册
     */
    RegisterResponse register(RegisterRequest request);
}