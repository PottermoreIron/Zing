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
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailPasswordRegisterStrategy extends AbstractRegisterStrategyImpl {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public EmailPasswordRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    @Override
    protected void validateCredential(RegistrationContext context) {
        var request = context.request();
        boolean codeValid = verificationCodeService.verifyCode(
                request.email(),
                VerificationCode.of(request.verificationCode()));
        if (!codeValid) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
    }

    @Override
    protected void beforeRegister(RegistrationContext context) {
        var request = context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        if (userModulePort.existsByEmail(Email.of(request.email()))) {
            throw new DomainException(AuthResultCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUser(RegistrationContext context) {
        var request = context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .email(Email.of(request.email()))
                .password(Password.of(request.password()))
                .build();
        var userId = userModulePort.createUser(createCommand);
        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    protected void afterRegister(UserDTO user, RegistrationContext context) {
        var request = context.request();
        verificationCodeService.deleteCode(request.email());
    }

    @Override
    public RegisterType getSupportedRegisterType() {
        return RegisterType.EMAIL_PASSWORD;
    }
}