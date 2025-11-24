package com.pot.auth.domain.strategy.login;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.strategy.AbstractLoginStrategyImpl;
import com.pot.auth.interfaces.dto.auth.UsernamePasswordLoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 用户名密码登录策略
 *
 * <p>通过用户名和密码进行登录认证
 *
 * @author yecao
 * @since 2025-11-19
 */
@Slf4j
@Component
public class UsernamePasswordLoginStrategy extends AbstractLoginStrategyImpl<UsernamePasswordLoginRequest> {

    private final UserModulePortFactory userModulePortFactory;

    public UsernamePasswordLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory
    ) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
    }

    @Override
    protected void validateRequest(UsernamePasswordLoginRequest request) {
        // Jakarta Validation已在Controller层完成，这里可以添加额外的业务验证
    }

    @Override
    protected UserDTO doLogin(UsernamePasswordLoginRequest request, LoginContext loginContext) {
        log.info("[用户名登录] 开始登录: username={}", request.username());

        // 1. 获取用户模块适配器
        UserDomain userDomain = request.userDomain();
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 2. 调用用户模块进行密码验证
        Optional<UserDTO> userOpt = userModulePort.authenticateWithPassword(request.username(), request.password());

        if (userOpt.isEmpty()) {
            log.warn("[用户名登录] 认证失败: username={}", request.username());
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }

        UserDTO user = userOpt.get();

        // 3. 检查用户状态
        validateUserStatus(user);

        log.info("[用户名登录] 登录成功: userId={}", user.userId());
        return user;
    }

    @Override
    protected LoginType getSupportedLoginType() {
        return LoginType.USERNAME_PASSWORD;
    }

    @Override
    public boolean supports(LoginType loginType) {
        return LoginType.USERNAME_PASSWORD.equals(loginType);
    }

    /**
     * 验证用户状态
     */
    private void validateUserStatus(UserDTO user) {
        if ("LOCKED".equals(user.status())) {
            log.warn("[用户名登录] 用户已被锁定: userId={}", user.userId());
            throw new DomainException(AuthResultCode.ACCOUNT_LOCKED);
        }

        if ("DISABLED".equals(user.status())) {
            log.warn("[用户名登录] 用户已被禁用: userId={}", user.userId());
            throw new DomainException(AuthResultCode.ACCOUNT_DISABLED);
        }
    }
}
