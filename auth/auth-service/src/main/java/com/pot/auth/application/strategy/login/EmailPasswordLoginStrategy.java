package com.pot.auth.application.strategy.login;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.application.strategy.AbstractLoginStrategyImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Login strategy for email and password credentials.
 */
@Slf4j
@Component
public class EmailPasswordLoginStrategy extends AbstractLoginStrategyImpl {

    private static final String AUTHENTICATED_USER_KEY = "authenticatedUser";

    private final UserModulePortFactory userModulePortFactory;

    public EmailPasswordLoginStrategy(
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
        log.debug("[邮箱密码登录] 验证凭证: email={}", request.email());

        UserModulePort port = userModulePortFactory.getPort(request.userDomain());
        UserDTO user = port.authenticateWithPassword(request.email(), request.password())
                .orElseThrow(() -> new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        context.withExtraAttribute(AUTHENTICATED_USER_KEY, user);
        log.debug("[邮箱密码登录] 凭证验证通过: email={}", request.email());
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
                    .findByEmail(request.email())
                    .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
        }
        return user;
    }

    @Override
    public LoginType getSupportedLoginType() {
        return LoginType.EMAIL_PASSWORD;
    }
}
