package com.pot.auth.application.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.validation.handler.UserStatusValidator;
import lombok.extern.slf4j.Slf4j;

/**
 * Base template for login strategies.
 */
@Slf4j
public abstract class AbstractLoginStrategyImpl implements LoginStrategy {

    protected final JwtTokenService jwtTokenService;

    protected AbstractLoginStrategyImpl(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public final AuthenticationResult execute(AuthenticationContext context) {
        var request = context.request();

        log.info("[登录策略] 开始执行登录: type={}, userDomain={}, ip={}",
                request.loginType(), request.userDomain(), context.ipAddress().value());

        try {
            validateCredential(context);
            UserDTO user = getUserInfo(context);
            UserStatusValidator.validate(user);
            beforeLogin(user, context);
            AuthenticationResult result = generateAuthenticationResult(user, context);
            afterLogin(user, result, context);

            log.info("[登录策略] 登录成功: userId={}, nickname={}, type={}",
                    user.userId(), user.nickname(), request.loginType());

            return result;

        } catch (Exception e) {
            handleLoginFailure(context, e);
            throw e;
        }
    }

    /**
     * Validates credentials for the current login type.
     */
    protected abstract void validateCredential(AuthenticationContext context);

    /**
     * Loads the authenticated user.
     */
    protected abstract UserDTO getUserInfo(AuthenticationContext context);

    /**
     * Hook invoked before token generation.
     */
    protected void beforeLogin(UserDTO user, AuthenticationContext context) {
        log.debug("[登录钩子] 登录前置处理: userId={}", user.userId());
    }

    /**
     * Builds the authentication result with issued tokens.
     */
    protected AuthenticationResult generateAuthenticationResult(
            UserDTO user,
            AuthenticationContext context) {
        var tokenPair = jwtTokenService.generateTokenPair(
                user.userId(),
                context.request().userDomain(),
                user.nickname(),
                user.permissions());

        return AuthenticationResult.builder()
                .userId(user.userId())
                .userDomain(context.request().userDomain())
                .nickname(user.nickname())
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
     * Hook invoked after a successful login.
     */
    protected void afterLogin(UserDTO user, AuthenticationResult result, AuthenticationContext context) {
        log.debug("[登录钩子] 登录后置处理: userId={}", user.userId());
    }

    /**
     * Handles a failed login attempt.
     */
    protected void handleLoginFailure(AuthenticationContext context, Exception e) {
        log.error("[登录策略] 登录失败: type={}, ip={}, error={}",
                context.request().loginType(),
                context.ipAddress().value(),
                e.getMessage());
    }

    /**
     * Returns the login type supported by this strategy.
     */
    public abstract LoginType getSupportedLoginType();
}
