package com.pot.auth.domain.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.validation.handler.UserStatusValidator;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 一键认证策略抽象模板类
 *
 * <p>
 * 采用模板方法模式，定义统一的一键认证流程：
 * <ol>
 * <li>责任链校验（参数校验→业务规则校验→风控校验）</li>
 * <li>查找用户</li>
 * <li>用户已存在 → 验证凭证 → 登录</li>
 * <li>用户不存在 → 验证凭证 → 注册 → 登录</li>
 * <li>生成Token并构建结果</li>
 * <li>返回响应</li>
 * </ol>
 *
 * <p>
 * <strong>与传统登录策略的区别：</strong>
 * <ul>
 * <li>LoginStrategy: 用户不存在直接失败</li>
 * <li>OneStopAuthStrategy: 用户不存在则自动注册</li>
 * </ul>
 *
 * @param <T> 具体的认证请求类型，必须继承自 OneStopAuthRequest
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
public abstract class AbstractOneStopAuthStrategyImpl<T extends OneStopAuthRequest>
        implements OneStopAuthStrategy<T> {

    protected final JwtTokenService jwtTokenService;
    protected final ValidationChain<OneStopAuthContext> validationChain;
    protected final UserDefaultsGenerator userDefaultsGenerator;

    protected AbstractOneStopAuthStrategyImpl(
            JwtTokenService jwtTokenService,
            ValidationChain<OneStopAuthContext> validationChain,
            UserDefaultsGenerator userDefaultsGenerator) {
        this.jwtTokenService = jwtTokenService;
        this.validationChain = validationChain;
        this.userDefaultsGenerator = userDefaultsGenerator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final AuthenticationResult execute(OneStopAuthContext context) {
        T request = (T) context.request();

        log.info("[一键认证] 开始执行: authType={}, userDomain={}, ip={}",
                request.authType(), request.userDomain(), context.ipAddress().value());

        try {
            // 1. 责任链校验
            validationChain.validate(context);

            // 2. 查找用户
            UserDTO user = findUser(context);

            if (user != null) {
                // 3a. 用户已存在 → 登录流程
                log.info("[一键认证] 用户已存在，执行登录: userId={}, authType={}",
                        user.userId(), request.authType());

                return handleExistingUser(user, context);
            } else {
                // 3b. 用户不存在 → 注册流程
                log.info("[一键认证] 用户不存在，执行注册: authType={}", request.authType());

                return handleNewUser(context);
            }
        } catch (Exception e) {
            log.error("[一键认证] 认证失败: authType={}, error={}",
                    request.authType(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理已存在的用户（登录流程）
     */
    private AuthenticationResult handleExistingUser(UserDTO user, OneStopAuthContext context) {
        // 1. 登录时的凭证验证
        validateCredentialForLogin(context, user);

        // 2. 用户状态验证
        UserStatusValidator.validate(user);

        // 3. 登录前置钩子
        beforeLogin(user, context);

        // 4. 生成Token
        AuthenticationResult result = generateAuthenticationResult(user, context);

        // 5. 登录后置钩子
        afterLogin(user, result, context);

        log.info("[一键认证] 登录成功: userId={}, username={}", user.userId(), user.username());
        return result;
    }

    /**
     * 处理新用户（注册流程）
     */
    private AuthenticationResult handleNewUser(OneStopAuthContext context) {
        // 1. 注册时的凭证验证
        validateCredentialForRegister(context);

        // 2. 注册前置钩子
        beforeRegister(context);

        // 3. 创建用户（自动生成默认值）
        UserDTO user = createUserWithDefaults(context);

        // 4. 注册后置钩子
        afterRegister(user, context);

        // 5. 生成Token
        AuthenticationResult result = generateAuthenticationResult(user, context);

        log.info("[一键认证] 注册并登录成功: userId={}, username={}", user.userId(), user.username());
        return result;
    }

    // ==================== 必须由子类实现的方法 ====================

    /**
     * 查找用户
     *
     * <p>
     * 根据不同的认证方式，查找用户的方式也不同：
     * <ul>
     * <li>手机号认证：通过手机号查找</li>
     * <li>邮箱认证：通过邮箱查找</li>
     * <li>用户名认证：通过用户名查找</li>
     * </ul>
     *
     * @param context 认证上下文
     * @return 用户信息，不存在则返回null
     */
    protected abstract UserDTO findUser(OneStopAuthContext context);

    /**
     * 登录时的凭证验证
     *
     * <p>
     * 用于验证已存在用户的凭证：
     * <ul>
     * <li>密码认证：验证密码</li>
     * <li>验证码认证：验证验证码（可选）</li>
     * </ul>
     *
     * @param context 认证上下文
     * @param user    用户信息
     */
    protected abstract void validateCredentialForLogin(OneStopAuthContext context, UserDTO user);

    /**
     * 注册时的凭证验证
     *
     * <p>
     * 用于验证新用户的凭证（通常是验证码）：
     * <ul>
     * <li>手机号注册：验证手机验证码</li>
     * <li>邮箱注册：验证邮箱验证码</li>
     * </ul>
     *
     * @param context 认证上下文
     */
    protected abstract void validateCredentialForRegister(OneStopAuthContext context);

    /**
     * 创建用户（自动生成默认值）
     *
     * <p>
     * 子类实现此方法时，应该：
     * <ul>
     * <li>如果用户未提供用户名，使用 userDefaultsGenerator 生成</li>
     * <li>如果用户未提供密码，使用 userDefaultsGenerator 生成</li>
     * <li>如果用户未提供头像，使用 userDefaultsGenerator 提供默认头像</li>
     * </ul>
     *
     * @param context 认证上下文
     * @return 新创建的用户信息
     */
    protected abstract UserDTO createUserWithDefaults(OneStopAuthContext context);

    // ==================== 可选的钩子方法 ====================

    /**
     * 登录前置钩子（由子类可选实现）
     *
     * @param user    用户信息
     * @param context 认证上下文
     */
    protected void beforeLogin(UserDTO user, OneStopAuthContext context) {
        log.debug("[一键认证钩子] 登录前置处理: userId={}", user.userId());
    }

    /**
     * 登录后置钩子（由子类可选实现）
     *
     * @param user    用户信息
     * @param result  认证结果
     * @param context 认证上下文
     */
    protected void afterLogin(UserDTO user, AuthenticationResult result, OneStopAuthContext context) {
        log.debug("[一键认证钩子] 登录后置处理: userId={}", user.userId());
    }

    /**
     * 注册前置钩子（由子类可选实现）
     *
     * @param context 认证上下文
     */
    protected void beforeRegister(OneStopAuthContext context) {
        log.debug("[一键认证钩子] 注册前置处理");
    }

    /**
     * 注册后置钩子（由子类可选实现）
     *
     * @param user    新创建的用户
     * @param context 认证上下文
     */
    protected void afterRegister(UserDTO user, OneStopAuthContext context) {
        log.debug("[一键认证钩子] 注册后置处理: userId={}", user.userId());
    }

    // ==================== 通用方法 ====================

    /**
     * 生成认证结果（包含Token）
     */
    protected AuthenticationResult generateAuthenticationResult(
            UserDTO user,
            OneStopAuthContext context) {
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

    @Override
    public boolean supports(AuthType authType) {
        return getSupportedAuthType() == authType;
    }
}
