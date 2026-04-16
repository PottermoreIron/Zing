package com.pot.auth.application.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.validation.handler.UserStatusValidator;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Base template for authenticate-or-register flows.
 */
@Slf4j
public abstract class AbstractOneStopAuthStrategyImpl implements OneStopAuthStrategy {

    protected final JwtTokenService jwtTokenService;
    protected final UserDefaultsGenerator userDefaultsGenerator;

    protected AbstractOneStopAuthStrategyImpl(
            JwtTokenService jwtTokenService,
            UserDefaultsGenerator userDefaultsGenerator) {
        this.jwtTokenService = jwtTokenService;
        this.userDefaultsGenerator = userDefaultsGenerator;
    }

    @Override
    public final AuthenticationResult execute(OneStopAuthContext context) {
        var request = context.request();

        log.info("[OneStopAuth] Executing — authType={}, userDomain={}, ip={}",
                request.authType(), request.userDomain(), context.ipAddress().value());

        try {
            UserDTO user = findUser(context);

            if (user != null) {
                log.info("[OneStopAuth] User found, executing login — userId={}, authType={}",
                        user.userId(), request.authType());

                return handleExistingUser(user, context);
            } else {
                log.info("[OneStopAuth] User not found, executing registration — authType={}", request.authType());

                return handleNewUser(context);
            }
        } catch (DomainException e) {
            log.warn("[OneStopAuth] Authentication failed — authType={}, error={}",
                    request.authType(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[OneStopAuth] Unexpected error — authType={}, error={}",
                    request.authType(), e.getMessage(), e);
            throw e;
        } finally {
            cleanupAfterAuthentication();
        }
    }

    /**
     * Handles an existing user with a login flow.
     */
    private AuthenticationResult handleExistingUser(UserDTO user, OneStopAuthContext context) {
        validateCredentialForLogin(context, user);
        UserStatusValidator.validate(user);
        beforeLogin(user, context);
        AuthenticationResult result = generateAuthenticationResult(user, context);
        afterLogin(user, result, context);

        log.info("[OneStopAuth] Login successful — userId={}, nickname={}", user.userId(), user.nickname());
        return result;
    }

    /**
     * Handles a new user with a register-and-login flow.
     */
    private AuthenticationResult handleNewUser(OneStopAuthContext context) {
        validateCredentialForRegister(context);
        beforeRegister(context);
        UserDTO user = createUserWithDefaults(context);
        afterRegister(user, context);
        AuthenticationResult result = generateAuthenticationResult(user, context);

        log.info("[OneStopAuth] Registration and login successful — userId={}, nickname={}", user.userId(),
                user.nickname());
        return result;
    }

    /**
     * Finds the user targeted by the authentication request.
     */
    protected abstract UserDTO findUser(OneStopAuthContext context);

    /**
     * Validates credentials for an existing user.
     */
    protected abstract void validateCredentialForLogin(OneStopAuthContext context, UserDTO user);

    /**
     * Validates credentials before registration.
     */
    protected abstract void validateCredentialForRegister(OneStopAuthContext context);

    /**
     * Creates a user and fills in generated defaults where needed.
     */
    protected abstract UserDTO createUserWithDefaults(OneStopAuthContext context);

    /**
     * Hook invoked before login token generation.
     */
    protected void beforeLogin(UserDTO user, OneStopAuthContext context) {
        log.debug("[OneStopAuthHook] Pre-login processing — userId={}", user.userId());
    }

    /**
     * Hook invoked after a successful login.
     */
    protected void afterLogin(UserDTO user, AuthenticationResult result, OneStopAuthContext context) {
        log.debug("[OneStopAuthHook] Post-login processing — userId={}", user.userId());
    }

    /**
     * Hook invoked before registration.
     */
    protected void beforeRegister(OneStopAuthContext context) {
        log.debug("[OneStopAuthHook] Pre-registration processing");
    }

    /**
     * Hook invoked after registration.
     */
    protected void afterRegister(UserDTO user, OneStopAuthContext context) {
        log.debug("[OneStopAuthHook] Post-registration processing — userId={}", user.userId());
    }

    /**
     * Builds the authentication result with issued tokens.
     */
    protected AuthenticationResult generateAuthenticationResult(
            UserDTO user,
            OneStopAuthContext context) {
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
     * Hook for releasing request-scoped resources after authentication.
     */
    protected void cleanupAfterAuthentication() {
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

    public abstract AuthType getSupportedAuthType();

}
