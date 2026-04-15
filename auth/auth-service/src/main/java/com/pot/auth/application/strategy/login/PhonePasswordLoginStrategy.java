package com.pot.auth.application.strategy.login;

import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.application.strategy.AbstractLoginStrategyImpl;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Login strategy for phone and password credentials.
 */
@Slf4j
@Component
public class PhonePasswordLoginStrategy extends AbstractLoginStrategyImpl {

    private static final String AUTHENTICATED_USER_KEY = "authenticatedUser";

    private final UserModulePortFactory userModulePortFactory;

    public PhonePasswordLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
    }

    /**
     * Caches the authenticated user to avoid a second user lookup.
     */
    @Override
    protected void validateCredential(AuthenticationContext context) {
        var request = context.request();
        log.debug("[PhonePasswordLogin] Verifying credentials — phone={}", request.phone());

        UserModulePort port = userModulePortFactory.getPort(request.userDomain());
        UserDTO user = port.authenticateWithPassword(request.phone(), request.password())
                .orElseThrow(() -> new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        context.withExtraAttribute(AUTHENTICATED_USER_KEY, user);
        log.debug("[PhonePasswordLogin] Credentials verified — phone={}", request.phone());
    }

    /**
     * Reuses the cached authenticated user when available.
     */
    @Override
    protected UserDTO getUserInfo(AuthenticationContext context) {
        UserDTO user = (UserDTO) context.getExtraAttribute(AUTHENTICATED_USER_KEY);
        if (user == null) {
            var request = context.request();
            user = userModulePortFactory.getPort(request.userDomain())
                    .findByPhone(request.phone())
                    .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
        }
        return user;
    }

    @Override
    public LoginType getSupportedLoginType() {
        return LoginType.PHONE_PASSWORD;
    }
}
