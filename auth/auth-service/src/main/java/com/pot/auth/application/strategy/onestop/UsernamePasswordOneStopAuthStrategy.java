package com.pot.auth.application.strategy.onestop;

import com.pot.auth.application.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.interfaces.dto.onestop.UsernamePasswordAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        var authResult = userModulePort.authenticateWithPassword(request.username(), request.password());
        if (authResult.isEmpty()) {
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        throw new DomainException(AuthResultCode.USER_NOT_FOUND);
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        throw new DomainException(AuthResultCode.USER_NOT_FOUND);
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.USERNAME_PASSWORD;
    }
}