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
 * 邮箱密码登录策略
 *
 * <p>
 * 通过邮箱和密码进行登录，authenticateWithPassword 内部完成验证+查询，
 * 避免两次 RPC 调用。
 *
 * @author pot
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
     * 验证邮箱密码，并将已认证的 UserDTO 放入 context extraAttributes，
     * 避免 getUserInfo() 再次发起 RPC。
     */
    @Override
    protected void validateCredential(AuthenticationContext context) {
        var request = context.request();
        log.debug("[邮箱密码登录] 验证凭证: email={}", request.email());

        UserModulePort port = userModulePortFactory.getPort(request.userDomain());
        UserDTO user = port.authenticateWithPassword(request.email(), request.password())
                .orElseThrow(() -> new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        // 将已认证用户暂存到 context，供 getUserInfo() 直接取用
        context.withExtraAttribute(AUTHENTICATED_USER_KEY, user);
        log.debug("[邮箱密码登录] 凭证验证通过: email={}", request.email());
    }

    /**
     * 直接从 context 取出已验证的 UserDTO，不重复发起 RPC。
     */
    @Override
    protected UserDTO getUserInfo(AuthenticationContext context) {
        UserDTO user = (UserDTO) context.getExtraAttribute(AUTHENTICATED_USER_KEY);
        if (user == null) {
            // 防御性兜底：理论上不会到这里
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
