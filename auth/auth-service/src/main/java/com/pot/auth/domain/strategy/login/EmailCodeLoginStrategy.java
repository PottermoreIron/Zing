package com.pot.auth.domain.strategy.login;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
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
import com.pot.auth.interfaces.dto.auth.EmailCodeLoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 邮箱验证码登录策略（重构版）
 *
 * <p>
 * 通过邮箱和验证码进行登录认证
 * <p>
 * 验证码登录特点：无需密码，通过验证码即可登录
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class EmailCodeLoginStrategy extends AbstractLoginStrategyImpl<EmailCodeLoginRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public EmailCodeLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService) {
        super(jwtTokenService, createValidationChain());
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    private static ValidationChain<AuthenticationContext> createValidationChain() {
        ValidationChain<AuthenticationContext> chain = new ValidationChain<>();
        chain.addHandler(new AuthenticationParameterValidator());
        return chain;
    }

    @Override
    protected void validateCredential(AuthenticationContext context) {
        EmailCodeLoginRequest request = (EmailCodeLoginRequest) context.request();

        log.debug("[邮箱验证码登录] 开始验证凭证: email={}", request.email());

        // 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(
                request.email(),
                request.verificationCode());

        if (!codeValid) {
            log.warn("[邮箱验证码登录] 验证码验证失败: email={}", request.email());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        log.debug("[邮箱验证码登录] 验证码验证通过: email={}", request.email());
    }

    @Override
    protected UserDTO getUserInfo(AuthenticationContext context) {
        EmailCodeLoginRequest request = (EmailCodeLoginRequest) context.request();

        log.debug("[邮箱验证码登录] 获取用户信息: email={}", request.email());

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        Optional<UserDTO> userOpt = userModulePort.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            log.error("[邮箱验证码登录] 用户不存在: email={}", request.email());
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected void afterLogin(UserDTO user, com.pot.auth.domain.authentication.entity.AuthenticationResult result,
                              AuthenticationContext context) {
        EmailCodeLoginRequest request = (EmailCodeLoginRequest) context.request();

        // 登录成功后清理验证码
        verificationCodeService.deleteCode(request.email());

        log.debug("[邮箱验证码登录] 已清理验证码: email={}", request.email());
    }

    @Override
    protected LoginType getSupportedLoginType() {
        return LoginType.EMAIL_CODE;
    }
}
