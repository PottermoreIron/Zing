package com.pot.auth.application.strategy.register;

import com.pot.auth.application.strategy.AbstractRegisterStrategyImpl;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.context.RegistrationContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.Phone;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.validation.handler.RegistrationParameterValidator;
import com.pot.auth.interfaces.dto.register.PhoneCodeRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PhoneCodeRegisterStrategy extends AbstractRegisterStrategyImpl<PhoneCodeRegisterRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public PhoneCodeRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService) {
        super(jwtTokenService, createValidationChain());
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    private static ValidationChain<RegistrationContext> createValidationChain() {
        ValidationChain<RegistrationContext> chain = new ValidationChain<>();
        chain.addHandler(new RegistrationParameterValidator());
        return chain;
    }

    @Override
    protected void validateCredential(RegistrationContext context) {
        PhoneCodeRegisterRequest request = (PhoneCodeRegisterRequest) context.request();
        boolean codeValid = verificationCodeService.verifyCode(
                request.phone(),
                VerificationCode.of(request.verificationCode()));
        if (!codeValid) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
    }

    @Override
    protected void beforeRegister(RegistrationContext context) {
        PhoneCodeRegisterRequest request = (PhoneCodeRegisterRequest) context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        if (userModulePort.existsByPhone(Phone.of(request.phone()))) {
            throw new DomainException(AuthResultCode.PHONE_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUser(RegistrationContext context) {
        PhoneCodeRegisterRequest request = (PhoneCodeRegisterRequest) context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .phone(Phone.of(request.phone()))
                .build();
        var userId = userModulePort.createUser(createCommand);
        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    protected void afterRegister(UserDTO user, RegistrationContext context) {
        PhoneCodeRegisterRequest request = (PhoneCodeRegisterRequest) context.request();
        verificationCodeService.deleteCode(request.phone());
    }

    @Override
    protected RegisterType getSupportedRegisterType() {
        return RegisterType.PHONE_CODE;
    }
}