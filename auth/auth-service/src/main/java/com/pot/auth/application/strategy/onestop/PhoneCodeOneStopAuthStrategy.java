package com.pot.auth.application.strategy.onestop;

import com.pot.auth.application.strategy.AbstractOneStopAuthStrategyImpl;
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
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.Phone;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PhoneCodeOneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public PhoneCodeOneStopAuthStrategy(
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
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        return userModulePort.findByPhone(request.phone()).orElse(null);
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        var request = context.request();
        boolean codeValid = verificationCodeService.verifyCode(
                request.phone(),
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
    protected void beforeRegister(OneStopAuthContext context) {
        var request = context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        if (userModulePort.existsByPhone(Phone.of(request.phone()))) {
            throw new DomainException(AuthResultCode.PHONE_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        var request = context.request();
        String password = userDefaultsGenerator.generateRandomPassword();
        String avatarUrl = userDefaultsGenerator.getDefaultAvatarUrl();

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        String generatedNickname = generateAvailableNickname(
                userModulePort,
                () -> userDefaultsGenerator.generateNicknameFromPhone(request.phone()));
        CreateUserCommand command = CreateUserCommand.builder()
                .phone(Phone.of(request.phone()))
                .password(Password.of(password))
                .nickname(generatedNickname)
                .avatarUrl(avatarUrl)
                .build();

        var userId = userModulePort.createUser(command);
        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    protected void afterRegister(UserDTO user, OneStopAuthContext context) {
        verificationCodeService.deleteCode(context.request().phone());
    }

    @Override
    protected void afterLogin(UserDTO user, AuthenticationResult result, OneStopAuthContext context) {
        verificationCodeService.deleteCode(context.request().phone());
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.PHONE_CODE;
    }
}