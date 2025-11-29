package com.pot.auth.domain.strategy.onestop;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.interfaces.dto.onestop.UsernamePasswordAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户名密码一键认证策略
 *
 * <p>
 * 注意：用户名密码认证通常只支持登录，不支持注册
 * <p>
 * 因为注册时用户名由用户主动提供，不需要自动生成
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class UsernamePasswordOneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl<UsernamePasswordAuthRequest> {

    private final UserModulePortFactory userModulePortFactory;

    public UsernamePasswordOneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, createValidationChain(), userDefaultsGenerator);
        this.userModulePortFactory = userModulePortFactory;
    }

    private static ValidationChain<OneStopAuthContext> createValidationChain() {
        return new ValidationChain<>();
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        UsernamePasswordAuthRequest request = (UsernamePasswordAuthRequest) context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        return userModulePort.findByIdentifier(request.username()).orElse(null);
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        UsernamePasswordAuthRequest request = (UsernamePasswordAuthRequest) context.request();

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        var authResult = userModulePort.authenticateWithPassword(
                request.username(), request.password());

        if (authResult.isEmpty()) {
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        // 用户名密码认证不支持自动注册
        throw new DomainException(AuthResultCode.USER_NOT_FOUND);
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        // 用户名密码认证不支持自动注册
        throw new DomainException(AuthResultCode.USER_NOT_FOUND);
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.USERNAME_PASSWORD;
    }
}
