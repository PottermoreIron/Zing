package com.pot.auth.domain.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.context.AuthenticationContext;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.interfaces.dto.auth.LoginRequest;

/**
 * 登录策略接口（重构版）
 *
 * <p>
 * 定义登录策略的核心方法，所有登录策略必须实现此接口
 * <p>
 * 采用策略模式，将不同登录方式的业务逻辑封装到各自的策略实现类中
 *
 * @param <T> 具体的登录请求类型，必须继承自 LoginRequest
 * @author pot
 * @since 2025-11-29
 */
public interface LoginStrategy<T extends LoginRequest> {

    /**
     * 执行登录逻辑
     *
     * @param context 认证上下文（包含登录请求、IP、设备信息等）
     * @return 认证结果（包含Token）
     */
    AuthenticationResult execute(AuthenticationContext context);

    /**
     * 判断该策略是否支持指定的登录类型
     *
     * @param loginType 登录类型
     * @return true if支持, false otherwise
     */
    boolean supports(LoginType loginType);
}
