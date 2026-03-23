package com.pot.auth.application.strategy.login;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.AuthenticationContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.application.strategy.AbstractLoginStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.validation.handler.AuthenticationParameterValidator;
import com.pot.auth.interfaces.dto.auth.UsernamePasswordLoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户名密码登录策略
 *
 * <p>通过用户名和密码进行登录，authenticateWithPassword 内部完成验证+查询，
 * 避免两次 RPC 调用。
 *
 * @author pot
 */
@Slf4j
@Component
public class UsernamePasswordLoginStrategy extends AbstractLoginStrategyImpl<UsernamePasswordLoginRequest> {

    private static final String AUTHENTICATED_USER_KEY = "authenticatedUser";

    private final UserModulePortFactory userModulePortFactory;

    public UsernamePasswordLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory) {
        super(jwtTokenService, buildValidationChain());
        this.userModulePortFactory = userModulePortFactory;
    }

    private static ValidationChain<AuthenticationContext> buildValidationChain() {
        ValidationChain<AuthenticationContext> chain = new ValidationChain<>();
        chain.addHandler(new AuthenticationParameterValidator());
        return chain;
    }

    @Override
    protected void validateCredential(AuthenticationContext context) {
        UsernamePasswordLoginRequest request = (UsernamePasswordLoginRequest) context.request();
        log.debug("[用户名密码登录] 验证凭证: username={}", request.username());

        UserModulePort port = userModulePortFactory.getPort(request.userDomain());
        UserDTO user = port.authenticateWithPassword(request.username(), request.password())
                .orElseThrow(() -> new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        context.withExtraAttribute(AUTHENTICATED_USER_KEY, user);
        log.debug("[用户名密码登录] 凭证验证通过: username={}", request.username());
    }

    @Override
    protected UserDTO getUserInfo(AuthenticationContext context) {
        UserDTO user = (UserDTO) context.getExtraAttribute(AUTHENTICATED_USER_KEY);
        if (user == null) {
            UsernamePasswordLoginRequest request = (UsernamePasswordLoginRequest) context.request();
            user = userModulePortFactory.getPort(request.userDomain())
                    .findByIdentifier(request.username())
                    .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
        }
        return user;
    }

    @Override
    protected LoginType getSupportedLoginType() {
        return LoginType.USERNAME_PASSWORD;
    }
}
