package com.pot.auth.domain.strategy.onestop;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.context.OneStopAuthContext;
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
import com.pot.auth.domain.strategy.AbstractOneStopAuthStrategyImpl;
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
public class EmailCodeOneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl<EmailCodeAuthRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public EmailCodeOneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, createValidationChain(), userDefaultsGenerator);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    private static ValidationChain<OneStopAuthContext> createValidationChain() {
        return new ValidationChain<>();
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        EmailCodeAuthRequest request = (EmailCodeAuthRequest) context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        return userModulePort.findByEmail(request.email()).orElse(null);
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        EmailCodeAuthRequest request = (EmailCodeAuthRequest) context.request();

        boolean codeValid = verificationCodeService.verifyCode(
                request.email(),
                VerificationCode.of(request.verificationCode()));

        if (!codeValid) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        validateCredentialForLogin(context, null);
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        EmailCodeAuthRequest request = (EmailCodeAuthRequest) context.request();

        String username = userDefaultsGenerator.generateUsernameFromEmail(request.email());
        String password = userDefaultsGenerator.generateRandomPassword();
        String avatarUrl = userDefaultsGenerator.getDefaultAvatarUrl();

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        CreateUserCommand command = CreateUserCommand.builder()
                .email(Email.of(request.email()))
                .password(Password.of(password))
                .username(username)
                .avatarUrl(avatarUrl)
                .build();

        var userId = userModulePort.createUser(command);

        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    protected void afterRegister(UserDTO user, OneStopAuthContext context) {
        EmailCodeAuthRequest request = (EmailCodeAuthRequest) context.request();
        verificationCodeService.deleteCode(request.email());
    }

    @Override
    protected void afterLogin(UserDTO user, com.pot.auth.domain.authentication.entity.AuthenticationResult result,
                              OneStopAuthContext context) {
        EmailCodeAuthRequest request = (EmailCodeAuthRequest) context.request();
        verificationCodeService.deleteCode(request.email());
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.EMAIL_CODE;
    }
}
