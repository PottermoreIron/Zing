package com.pot.auth.domain.strategy.login;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.strategy.AbstractLoginStrategy;
import com.pot.auth.interfaces.dto.auth.EmailPasswordLoginRequest;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 邮箱密码登录策略
 *
 * <p>通过邮箱和密码进行登录认证
 *
 * @author yecao
 * @since 2025-11-19
 */
@Slf4j
@Component
public class EmailPasswordLoginStrategy extends AbstractLoginStrategy {

    private final UserModulePortFactory userModulePortFactory;

    public EmailPasswordLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory
    ) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
    }

    @Override
    protected void validateRequest(LoginRequest request) {
        if (!(request instanceof EmailPasswordLoginRequest)) {
            throw new DomainException(AuthResultCode.INVALID_LOGIN_REQUEST);
        }
    }

    @Override
    protected UserDTO doLogin(LoginRequest request, LoginContext loginContext) {
        EmailPasswordLoginRequest req = (EmailPasswordLoginRequest) request;

        log.info("[邮箱登录] 开始登录: email={}", req.email());

        // 1. 获取用户模块适配器
        UserDomain userDomain = UserDomain.fromCode(req.userDomain());
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 2. 调用用户模块进行密码验证
        Optional<UserDTO> userOpt = userModulePort.authenticateWithPassword(req.email(), req.password());

        if (userOpt.isEmpty()) {
            log.warn("[邮箱登录] 认证失败: email={}", req.email());
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }

        UserDTO user = userOpt.get();

        // 3. 检查用户状态
        validateUserStatus(user);

        log.info("[邮箱登录] 登录成功: userId={}", user.userId());
        return user;
    }

    @Override
    protected String getSupportedLoginType() {
        return "EMAIL_PASSWORD";
    }

    @Override
    public boolean supports(String loginType) {
        return "EMAIL_PASSWORD".equals(loginType);
    }

    /**
     * 验证用户状态
     */
    private void validateUserStatus(UserDTO user) {
        if ("LOCKED".equals(user.status())) {
            log.warn("[邮箱登录] 用户已被锁定: userId={}", user.userId());
            throw new DomainException(AuthResultCode.ACCOUNT_LOCKED);
        }

        if ("DISABLED".equals(user.status())) {
            log.warn("[邮箱登录] 用户已被禁用: userId={}", user.userId());
            throw new DomainException(AuthResultCode.ACCOUNT_DISABLED);
        }
    }
}

