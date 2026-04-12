package com.pot.auth.application.validation.handler;

import com.pot.auth.application.command.RegisterCommand;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.application.validation.ValidationHandler;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.zing.framework.common.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RegistrationParameterValidator implements ValidationHandler<RegistrationContext> {

    @Override
    public void validate(RegistrationContext context) {
        RegisterCommand request = context.request();
        log.debug("[ParamValidator] Validating registration request — type={}", request.registerType());

        switch (request.registerType()) {
            case USERNAME_PASSWORD -> validateNicknamePassword(request);
            case EMAIL_PASSWORD -> validateEmailPassword(request);
            case EMAIL_CODE -> validateEmailCode(request);
            case PHONE_CODE -> validatePhoneCode(request);
            case OAUTH2, WECHAT -> {
            }
            default -> throw new DomainException("Unsupported registration type: " + request.registerType());
        }
    }

    private void validateNicknamePassword(RegisterCommand request) {
        if (!ValidationUtils.isValidNickname(request.nickname())) {
            throw new DomainException("Nickname is invalid");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("Password is invalid");
        }
    }

    private void validateEmailPassword(RegisterCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("Invalid email format");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("Password is invalid");
        }
    }

    private void validateEmailCode(RegisterCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("Invalid email format");
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException("Invalid verification code format");
        }
    }

    private void validatePhoneCode(RegisterCommand request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException("Invalid phone number format");
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException("Invalid verification code format");
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}