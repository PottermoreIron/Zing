package com.pot.auth.application.strategy.register;

import com.pot.auth.application.strategy.AbstractRegisterStrategyImpl;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.Phone;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PhoneCodeRegisterStrategy extends AbstractRegisterStrategyImpl {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;
    private final UserDefaultsGenerator userDefaultsGenerator;

    public PhoneCodeRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
        this.userDefaultsGenerator = userDefaultsGenerator;
    }

    @Override
    protected void validateCredential(RegistrationContext context) {
        var request = context.request();
        boolean codeValid = verificationCodeService.verifyCode(
                request.phone(),
                VerificationCode.of(request.verificationCode()));
        if (!codeValid) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
    }

    @Override
    protected void beforeRegister(RegistrationContext context) {
        var request = context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        if (userModulePort.existsByPhone(Phone.of(request.phone()))) {
            throw new DomainException(AuthResultCode.PHONE_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUser(RegistrationContext context) {
        var request = context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        String generatedNickname = generateAvailableNickname(
                userModulePort,
                () -> userDefaultsGenerator.generateNicknameFromPhone(request.phone()));
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .nickname(generatedNickname)
                .phone(Phone.of(request.phone()))
                .password(Password.of(userDefaultsGenerator.generateRandomPassword()))
                .build();
        var userId = userModulePort.createUser(createCommand);
        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    protected void afterRegister(UserDTO user, RegistrationContext context) {
        var request = context.request();
        verificationCodeService.deleteCode(request.phone());
    }

    @Override
    public RegisterType getSupportedRegisterType() {
        return RegisterType.PHONE_CODE;
    }
}