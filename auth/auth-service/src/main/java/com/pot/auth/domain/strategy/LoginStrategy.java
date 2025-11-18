package com.pot.auth.domain.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.interfaces.dto.auth.LoginRequest;

/**
 * 登录策略接口
 *
 * <p>定义登录策略的核心方法，所有登录策略必须实现此接口
 * <p>采用策略模式，将不同登录方式的业务逻辑封装到各自的策略实现类中
 *
 * @author yecao
 * @since 2025-11-18
 */
public interface LoginStrategy {

    /**
     * 执行登录逻辑
     *
     * @param request 登录请求
     * @param ipAddress 客户端IP地址
     * @param userAgent 用户代理信息
     * @return 认证结果（包含Token）
     */
    AuthenticationResult execute(LoginRequest request, String ipAddress, String userAgent);

    /**
     * 判断该策略是否支持指定的登录类型
     *
     * @param loginType 登录类型
     * @return true if支持, false otherwise
     */
    boolean supports(String loginType);
}

