package com.pot.auth.application.strategy.login;

import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.application.strategy.AbstractLoginStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.application.validation.handler.AuthenticationParameterValidator;
import com.pot.auth.interfaces.dto.auth.EmailCodeLoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码登录策略
 *
 * @author pot
 */
@Slf4j
@Component
public class EmailCodeLoginStrategy extends AbstractLoginStrategyImpl<EmailCodeLoginRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public EmailCodeLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService,
            AuthenticationParameterValidator authenticationParameterValidator) {
        super(jwtTokenService, buildValidationChain(authenticationParameterValidator));
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    private static ValidationChain<AuthenticationContext> buildValidationChain(
            AuthenticationParameterValidator authenticationParameterValidator) {
        ValidationChain<AuthenticationContext> chain = new ValidationChain<>();
        chain.addHandler(authenticationParameterValidator);
        return chain;
    }

    @Override
    protected void validateCredential(AuthenticationContext context) {
        EmailCodeLoginRequest request = (EmailCodeLoginRequest) context.request();
        log.debug("[邮箱验证码登录] 验证凭证: email={}", request.email());

        if (!verificationCodeService.verifyCode(request.email(), request.verificationCode())) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
    }

    @Override
    protected UserDTO getUserInfo(AuthenticationContext context) {
        EmailCodeLoginRequest request = (EmailCodeLoginRequest) context.request();
        return userModulePortFactory.getPort(request.userDomain())
                .findByEmail(request.email())
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    protected LoginType getSupportedLoginType() {
        return LoginType.EMAIL_CODE;
    }
}
