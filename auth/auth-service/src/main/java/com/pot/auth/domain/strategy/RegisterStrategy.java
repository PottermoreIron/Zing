package com.pot.auth.domain.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.context.RegistrationContext;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.interfaces.dto.register.RegisterRequest;

/**
 * 注册策略接口
 *
 * <p>
 * 定义注册策略的核心方法，所有注册策略必须实现此接口
 * <p>
 * 采用策略模式，将不同注册方式的业务逻辑封装到各自的策略实现类中
 *
 * @param <T> 具体的注册请求类型，必须继承自 RegisterRequest
 * @author pot
 * @since 2025-11-29
 */
public interface RegisterStrategy<T extends RegisterRequest> {

    /**
     * 执行注册逻辑
     *
     * @param context 注册上下文（包含注册请求、IP、设备信息等）
     * @return 认证结果（注册后自动登录，返回Token）
     */
    AuthenticationResult execute(RegistrationContext context);

    /**
     * 判断该策略是否支持指定的注册类型
     *
     * @param registerType 注册类型
     * @return true if支持, false otherwise
     */
    boolean supports(RegisterType registerType);
}
