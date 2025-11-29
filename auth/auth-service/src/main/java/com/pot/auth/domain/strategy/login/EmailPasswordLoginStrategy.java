package com.pot.auth.domain.strategy.login;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.AuthenticationContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.strategy.AbstractLoginStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.validation.handler.AuthenticationParameterValidator;
import com.pot.auth.interfaces.dto.auth.EmailPasswordLoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 邮箱密码登录策略（重构版）
 *
 * <p>
 * 通过邮箱和密码进行登录认证
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class EmailPasswordLoginStrategy extends AbstractLoginStrategyImpl<EmailPasswordLoginRequest> {

    private final UserModulePortFactory userModulePortFactory;

    public EmailPasswordLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory) {
        super(jwtTokenService, createValidationChain());
        this.userModulePortFactory = userModulePortFactory;
    }

    private static ValidationChain<AuthenticationContext> createValidationChain() {
        ValidationChain<AuthenticationContext> chain = new ValidationChain<>();
        chain.addHandler(new AuthenticationParameterValidator());
        return chain;
    }

    @Override
    protected void validateCredential(AuthenticationContext context) {
        EmailPasswordLoginRequest request = (EmailPasswordLoginRequest) context.request();

        log.debug("[邮箱登录] 开始验证凭证: email={}", request.email());

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        Optional<UserDTO> userOpt = userModulePort.authenticateWithPassword(
                request.email(),
                request.password());

        if (userOpt.isEmpty()) {
            log.warn("[邮箱登录] 密码验证失败: email={}", request.email());
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }

        log.debug("[邮箱登录] 凭证验证通过: email={}", request.email());
    }

    @Override
    protected UserDTO getUserInfo(AuthenticationContext context) {
        EmailPasswordLoginRequest request = (EmailPasswordLoginRequest) context.request();

        log.debug("[邮箱登录] 获取用户信息: email={}", request.email());

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        Optional<UserDTO> userOpt = userModulePort.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            log.error("[邮箱登录] 用户不存在: email={}", request.email());
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected LoginType getSupportedLoginType() {
        return LoginType.EMAIL_PASSWORD;
    }
}
