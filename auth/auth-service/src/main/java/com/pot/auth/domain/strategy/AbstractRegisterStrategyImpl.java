package com.pot.auth.domain.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.RegistrationContext;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.interfaces.dto.register.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 注册策略抽象模板类（重构版）
 *
 * <p>
 * 采用模板方法模式，定义统一的注册流程：
 * <ol>
 * <li>责任链校验（参数校验→业务规则校验→唯一性校验）</li>
 * <li>凭证验证（验证码等）</li>
 * <li>注册前置钩子（风控检查、邀请码验证等）</li>
 * <li>创建用户</li>
 * <li>注册后置钩子（发送欢迎邮件、初始化用户数据等）</li>
 * <li>生成Token并构建结果</li>
 * <li>返回响应</li>
 * </ol>
 *
 * @param <T> 具体的注册请求类型，必须继承自 RegisterRequest
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRegisterStrategyImpl<T extends RegisterRequest> implements RegisterStrategy<T> {

    protected final JwtTokenService jwtTokenService;
    protected final ValidationChain<RegistrationContext> validationChain;

    @Override
    @SuppressWarnings("unchecked")
    public final AuthenticationResult execute(RegistrationContext context) {
        T request = (T) context.request();

        log.info("[注册策略] 开始执行注册: type={}, userDomain={}, ip={}",
                request.registerType(), request.userDomain(), context.ipAddress().value());

        try {
            // 1. 责任链校验
            validationChain.validate(context);

            // 2. 凭证验证（验证码等）
            validateCredential(context);

            // 3. 注册前置钩子（风控检查、邀请码验证等）
            beforeRegister(context);

            // 4. 创建用户
            UserDTO user = createUser(context);

            // 5. 注册后置钩子（发送欢迎邮件、初始化用户数据等）
            afterRegister(user, context);

            // 6. 生成Token并构建结果
            AuthenticationResult result = generateAuthenticationResult(user, context);

            log.info("[注册策略] 注册成功: userId={}, username={}, type={}",
                    user.userId(), user.username(), request.registerType());

            return result;

        } catch (Exception e) {
            handleRegisterFailure(context, e);
            throw e;
        }
    }

    /**
     * 凭证验证（由子类实现）
     *
     * <p>
     * 验证注册凭证（验证码等）
     *
     * @param context 注册上下文
     */
    protected abstract void validateCredential(RegistrationContext context);

    /**
     * 创建用户（由子类实现）
     *
     * <p>
     * 执行用户创建逻辑
     *
     * @param context 注册上下文
     * @return 创建的用户信息
     */
    protected abstract UserDTO createUser(RegistrationContext context);

    /**
     * 注册前置钩子（由子类可选实现）
     *
     * <p>
     * 可用于：
     * <ul>
     * <li>风控检查</li>
     * <li>邀请码验证</li>
     * <li>IP黑名单检查</li>
     * </ul>
     *
     * @param context 注册上下文
     */
    protected void beforeRegister(RegistrationContext context) {
        // 默认实现为空，子类可选择性覆盖
        log.debug("[注册钩子] 注册前置处理");
    }

    /**
     * 注册后置钩子（由子类可选实现）
     *
     * <p>
     * 可用于：
     * <ul>
     * <li>发送欢迎邮件</li>
     * <li>初始化用户数据</li>
     * <li>发送注册事件</li>
     * <li>赠送新人礼包</li>
     * </ul>
     *
     * @param user    新创建的用户
     * @param context 注册上下文
     */
    protected void afterRegister(UserDTO user, RegistrationContext context) {
        // 默认实现为空，子类可选择性覆盖
        log.debug("[注册钩子] 注册后置处理: userId={}", user.userId());
    }

    /**
     * 生成认证结果（包含Token）
     */
    protected AuthenticationResult generateAuthenticationResult(
            UserDTO user,
            RegistrationContext context) {
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
     * 处理注册失败
     *
     * <p>
     * 记录失败日志，子类可扩展以实现注册失败统计等功能
     */
    protected void handleRegisterFailure(RegistrationContext context, Exception e) {
        log.error("[注册策略] 注册失败: type={}, ip={}, error={}",
                context.request().registerType(),
                context.ipAddress().value(),
                e.getMessage());
    }

    /**
     * 获取策略支持的注册类型（由子类实现）
     */
    protected abstract RegisterType getSupportedRegisterType();

    @Override
    public boolean supports(RegisterType registerType) {
        return getSupportedRegisterType().equals(registerType);
    }
}
