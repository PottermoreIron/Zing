package com.pot.auth.application.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Base template for register strategies.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRegisterStrategyImpl implements RegisterStrategy {

    protected final JwtTokenService jwtTokenService;

    @Override
    public final AuthenticationResult execute(RegistrationContext context) {
        var request = context.request();

        log.info("[注册策略] 开始执行注册: type={}, userDomain={}, ip={}",
                request.registerType(), request.userDomain(), context.ipAddress().value());

        try {
            validateCredential(context);
            beforeRegister(context);
            UserDTO user = createUser(context);
            afterRegister(user, context);
            AuthenticationResult result = generateAuthenticationResult(user, context);

            log.info("[注册策略] 注册成功: userId={}, nickname={}, type={}",
                    user.userId(), user.nickname(), request.registerType());

            return result;

        } catch (Exception e) {
            handleRegisterFailure(context, e);
            throw e;
        }
    }

    /**
     * Validates registration credentials.
     */
    protected abstract void validateCredential(RegistrationContext context);

    /**
     * Creates the user for the current registration flow.
     */
    protected abstract UserDTO createUser(RegistrationContext context);

    /**
     * Hook invoked before user creation.
     */
    protected void beforeRegister(RegistrationContext context) {
        log.debug("[注册钩子] 注册前置处理");
    }

    /**
     * Hook invoked after user creation.
     */
    protected void afterRegister(UserDTO user, RegistrationContext context) {
        log.debug("[注册钩子] 注册后置处理: userId={}", user.userId());
    }

    /**
     * Builds the authentication result with issued tokens.
     */
    protected AuthenticationResult generateAuthenticationResult(
            UserDTO user,
            RegistrationContext context) {
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
     * Handles a failed register attempt.
     */
    protected void handleRegisterFailure(RegistrationContext context, Exception e) {
        log.error("[注册策略] 注册失败: type={}, ip={}, error={}",
                context.request().registerType(),
                context.ipAddress().value(),
                e.getMessage());
    }

    protected String generateAvailableNickname(UserModulePort userModulePort, Supplier<String> candidateSupplier) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = candidateSupplier.get();
            if (!userModulePort.existsByNickname(candidate)) {
                return candidate;
            }
        }
        throw new DomainException(AuthResultCode.USERNAME_ALREADY_EXISTS);
    }

    /**
     * Returns the register type supported by this strategy.
     */
    public abstract RegisterType getSupportedRegisterType();
}
