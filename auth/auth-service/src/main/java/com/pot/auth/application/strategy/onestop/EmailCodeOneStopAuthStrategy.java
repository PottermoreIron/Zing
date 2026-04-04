package com.pot.auth.application.strategy.onestop;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import com.pot.auth.application.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.application.validation.handler.OneStopAuthenticationParameterValidator;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.interfaces.dto.onestop.EmailCodeAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码一键认证策略
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class EmailCodeOneStopAuthStrategy extends AbstractOneStopAuthStrategyImpl<EmailCodeAuthRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public EmailCodeOneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService,
            OneStopAuthenticationParameterValidator oneStopAuthenticationParameterValidator,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, createValidationChain(oneStopAuthenticationParameterValidator), userDefaultsGenerator);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    private static ValidationChain<OneStopAuthContext> createValidationChain(
            OneStopAuthenticationParameterValidator oneStopAuthenticationParameterValidator) {
        ValidationChain<OneStopAuthContext> chain = new ValidationChain<>();
        chain.addHandler(oneStopAuthenticationParameterValidator);
        return chain;
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        EmailCodeAuthRequest request = (EmailCodeAuthRequest) context.request();
        return userModulePortFactory.getPort(request.userDomain())
                .findByEmail(request.email())
                .orElse(null);
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        EmailCodeAuthRequest request = (EmailCodeAuthRequest) context.request();
        if (!verificationCodeService.verifyCode(request.email(), VerificationCode.of(request.verificationCode()))) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        validateCredentialForLogin(context, null);
    }

    @Override
    protected void beforeRegister(OneStopAuthContext context) {
        EmailCodeAuthRequest request = (EmailCodeAuthRequest) context.request();
        UserModulePort port = userModulePortFactory.getPort(request.userDomain());
        if (port.existsByEmail(Email.of(request.email()))) {
            throw new DomainException(AuthResultCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        EmailCodeAuthRequest request = (EmailCodeAuthRequest) context.request();
        UserModulePort port = userModulePortFactory.getPort(request.userDomain());
        String username = generateAvailableUsername(
                port,
                () -> userDefaultsGenerator.generateUsernameFromEmail(request.email()));

        var userId = port.createUser(CreateUserCommand.builder()
                .email(Email.of(request.email()))
                .password(Password.of(userDefaultsGenerator.generateRandomPassword()))
                .username(username)
                .avatarUrl(userDefaultsGenerator.getDefaultAvatarUrl())
                .build());

        return port.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    protected void afterRegister(UserDTO user, OneStopAuthContext context) {
        verificationCodeService.deleteCode(((EmailCodeAuthRequest) context.request()).email());
    }

    @Override
    protected void afterLogin(UserDTO user, AuthenticationResult result, OneStopAuthContext context) {
        verificationCodeService.deleteCode(((EmailCodeAuthRequest) context.request()).email());
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.EMAIL_CODE;
    }
}
