package com.pot.auth.domain.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.AuthenticationContext;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.validation.handler.UserStatusValidator;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录策略抽象模板类（重构版）
 *
 * <p>
 * 采用模板方法模式，定义统一的登录流程：
 * <ol>
 * <li>责任链校验（参数校验→业务规则校验→风控校验）</li>
 * <li>凭证验证（由子类实现）</li>
 * <li>获取用户信息（由子类实现）</li>
 * <li>用户状态验证</li>
 * <li>登录前置钩子（可选，供子类扩展）</li>
 * <li>生成Token并构建结果</li>
 * <li>登录后置钩子（可选，供子类扩展）</li>
 * <li>返回响应</li>
 * </ol>
 *
 * @param <T> 具体的登录请求类型，必须继承自 LoginRequest
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
public abstract class AbstractLoginStrategyImpl<T extends LoginRequest> implements LoginStrategy<T> {

    protected final JwtTokenService jwtTokenService;
    protected final ValidationChain<AuthenticationContext> validationChain;

    protected AbstractLoginStrategyImpl(
            JwtTokenService jwtTokenService,
            ValidationChain<AuthenticationContext> validationChain) {
        this.jwtTokenService = jwtTokenService;
        this.validationChain = validationChain;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final AuthenticationResult execute(AuthenticationContext context) {
        T request = (T) context.request();

        log.info("[登录策略] 开始执行登录: type={}, userDomain={}, ip={}",
                request.loginType(), request.userDomain(), context.ipAddress().value());

        try {
            // 1. 责任链校验
            validationChain.validate(context);

            // 2. 凭证验证（由子类实现）
            validateCredential(context);

            // 3. 获取用户信息（由子类实现）
            UserDTO user = getUserInfo(context);

            // 4. 用户状态验证（使用健壮的状态处理器）
            UserStatusValidator.validate(user);

            // 5. 登录前置钩子（由子类可选实现）
            beforeLogin(user, context);

            // 6. 生成Token并构建结果
            AuthenticationResult result = generateAuthenticationResult(user, context);

            // 7. 登录后置钩子（由子类可选实现）
            afterLogin(user, result, context);

            log.info("[登录策略] 登录成功: userId={}, username={}, type={}",
                    user.userId(), user.username(), request.loginType());

            return result;

        } catch (Exception e) {
            handleLoginFailure(context, e);
            throw e;
        }
    }

    /**
     * 凭证验证（由子类实现）
     *
     * <p>
     * 不同登录方式的凭证验证逻辑不同：
     * <ul>
     * <li>密码登录：验证密码哈希</li>
     * <li>验证码登录：验证验证码</li>
     * </ul>
     *
     * @param context 认证上下文
     */
    protected abstract void validateCredential(AuthenticationContext context);

    /**
     * 获取用户信息（由子类实现）
     *
     * <p>
     * 根据登录方式的不同，查询用户的方式也不同：
     * <ul>
     * <li>用户名登录：通过用户名查询</li>
     * <li>邮箱登录：通过邮箱查询</li>
     * <li>手机号登录：通过手机号查询</li>
     * </ul>
     *
     * @param context 认证上下文
     * @return 用户信息
     */
    protected abstract UserDTO getUserInfo(AuthenticationContext context);

    /**
     * 登录前置钩子（由子类可选实现）
     *
     * <p>
     * 可用于：
     * <ul>
     * <li>记录登录尝试</li>
     * <li>更新最后登录时间</li>
     * <li>清除之前的登录失败记录</li>
     * </ul>
     *
     * @param user    用户信息
     * @param context 认证上下文
     */
    protected void beforeLogin(UserDTO user, AuthenticationContext context) {
        // 默认实现为空，子类可选择性覆盖
        log.debug("[登录钩子] 登录前置处理: userId={}", user.userId());
    }

    /**
     * 生成认证结果（包含Token）
     */
    protected AuthenticationResult generateAuthenticationResult(
            UserDTO user,
            AuthenticationContext context) {
        var tokenPair = jwtTokenService.generateTokenPair(
                user.userId(),
                context.request().userDomain(),
                user.username(),
                user.permissions());

        return AuthenticationResult.builder()
                .userId(user.userId())
                .userDomain(context.request().userDomain())
                .username(user.username())
                .email(user.email())
                .phone(user.phone())
                .accessToken(tokenPair.accessToken().rawToken())
                .refreshToken(tokenPair.refreshToken().rawToken())
                .accessTokenExpiresAt(tokenPair.accessToken().expiresAt())
                .refreshTokenExpiresAt(tokenPair.refreshToken().expiresAt())
                .loginContext(com.pot.auth.domain.shared.valueobject.LoginContext.of(
                        context.ipAddress(),
                        context.deviceInfo()))
                .build();
    }

    /**
     * 登录后置钩子（由子类可选实现）
     *
     * <p>
     * 可用于：
     * <ul>
     * <li>清理验证码</li>
     * <li>发送登录通知</li>
     * <li>记录登录日志</li>
     * <li>触发登录事件</li>
     * </ul>
     *
     * @param user    用户信息
     * @param result  认证结果
     * @param context 认证上下文
     */
    protected void afterLogin(UserDTO user, AuthenticationResult result, AuthenticationContext context) {
        // 默认实现为空，子类可选择性覆盖
        log.debug("[登录钩子] 登录后置处理: userId={}", user.userId());
    }

    /**
     * 处理登录失败
     *
     * <p>
     * 记录失败日志，子类可扩展以实现登录失败次数限制等功能
     */
    protected void handleLoginFailure(AuthenticationContext context, Exception e) {
        log.error("[登录策略] 登录失败: type={}, ip={}, error={}",
                context.request().loginType(),
                context.ipAddress().value(),
                e.getMessage());
    }

    /**
     * 获取策略支持的登录类型（由子类实现）
     */
    protected abstract LoginType getSupportedLoginType();

    @Override
    public boolean supports(LoginType loginType) {
        return getSupportedLoginType().equals(loginType);
    }
}
