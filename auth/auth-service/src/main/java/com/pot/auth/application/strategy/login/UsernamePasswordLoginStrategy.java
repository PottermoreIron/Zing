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
 * 昵称密码登录策略
 *
 * <p>
 * 通过昵称和密码进行登录，authenticateWithPassword 内部完成验证+查询，
 * 避免两次 RPC 调用。
 *
 * @author pot
 */
@Slf4j
@Component
public class UsernamePasswordLoginStrategy extends AbstractLoginStrategyImpl {

    private static final String AUTHENTICATED_USER_KEY = "authenticatedUser";

    private final UserModulePortFactory userModulePortFactory;

    public UsernamePasswordLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
    }

    @Override
    protected void validateCredential(AuthenticationContext context) {
        var request = context.request();
        log.debug("[昵称密码登录] 验证凭证: nickname={}", request.nickname());

        UserModulePort port = userModulePortFactory.getPort(request.userDomain());
        UserDTO user = port.authenticateWithPassword(request.nickname(), request.password())
                .orElseThrow(() -> new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        context.withExtraAttribute(AUTHENTICATED_USER_KEY, user);
        log.debug("[昵称密码登录] 凭证验证通过: nickname={}", request.nickname());
    }

    @Override
    protected UserDTO getUserInfo(AuthenticationContext context) {
        UserDTO user = (UserDTO) context.getExtraAttribute(AUTHENTICATED_USER_KEY);
        if (user == null) {
            var request = context.request();
            user = userModulePortFactory.getPort(request.userDomain())
                    .findByIdentifier(request.nickname())
                    .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
        }
        return user;
    }

    @Override
    public LoginType getSupportedLoginType() {
        return LoginType.USERNAME_PASSWORD;
    }
}
