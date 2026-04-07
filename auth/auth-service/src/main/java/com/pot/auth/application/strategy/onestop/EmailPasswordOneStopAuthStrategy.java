package com.pot.auth.application.strategy.onestop;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * One-stop auth strategy for email and password credentials.
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class EmailPasswordOneStopAuthStrategy extends AbstractOneStopAuthStrategyImpl {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public EmailPasswordOneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, userDefaultsGenerator);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        var request = context.request();
        return userModulePortFactory.getPort(request.userDomain())
                .findByEmail(request.email())
                .orElse(null);
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        var request = context.request();
        if (!StringUtils.hasText(request.password())) {
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }
        if (userModulePortFactory.getPort(request.userDomain())
                .authenticateWithPassword(request.email(), request.password()).isEmpty()) {
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        var request = context.request();
        if (!StringUtils.hasText(request.verificationCode())) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
        if (!verificationCodeService.verifyCode(request.email(), VerificationCode.of(request.verificationCode()))) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
    }

    @Override
    protected void beforeRegister(OneStopAuthContext context) {
        var request = context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        if (userModulePort.existsByEmail(Email.of(request.email()))) {
            throw new DomainException(AuthResultCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        var request = context.request();
        String password = StringUtils.hasText(request.password())
                ? request.password()
                : userDefaultsGenerator.generateRandomPassword();
        UserModulePort port = userModulePortFactory.getPort(request.userDomain());
        String generatedNickname = generateAvailableNickname(
                port,
                () -> userDefaultsGenerator.generateNicknameFromEmail(request.email()));

        var userId = port.createUser(CreateUserCommand.builder()
                .email(Email.of(request.email()))
                .password(Password.of(password))
                .nickname(generatedNickname)
                .avatarUrl(userDefaultsGenerator.getDefaultAvatarUrl())
                .build());

        return port.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    protected void afterRegister(UserDTO user, OneStopAuthContext context) {
        var request = context.request();
        if (StringUtils.hasText(request.verificationCode())) {
            verificationCodeService.deleteCode(request.email());
        }
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.EMAIL_PASSWORD;
    }
}
