package com.pot.auth.application.validation.handler;

import com.pot.auth.application.command.RegisterCommand;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.application.validation.ValidationHandler;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.zing.framework.common.util.ValidationUtils;
import org.springframework.util.StringUtils;
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
            case PHONE_PASSWORD -> validatePhonePassword(request);
            case OAUTH2, WECHAT -> {
            }
            default -> throw new DomainException(AuthResultCode.UNSUPPORTED_REGISTER_TYPE);
        }
    }

    private void validateNicknamePassword(RegisterCommand request) {
        if (!ValidationUtils.isValidNickname(request.nickname())) {
            throw new DomainException(AuthResultCode.INVALID_USERNAME);
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException(AuthResultCode.INVALID_PASSWORD);
        }
    }

    private void validateEmailPassword(RegisterCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException(AuthResultCode.INVALID_EMAIL);
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException(AuthResultCode.INVALID_PASSWORD);
        }
        if (!StringUtils.hasText(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_REQUIRED);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validateEmailCode(RegisterCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException(AuthResultCode.INVALID_EMAIL);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validatePhoneCode(RegisterCommand request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException(AuthResultCode.INVALID_PHONE);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validatePhonePassword(RegisterCommand request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException(AuthResultCode.INVALID_PHONE);
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException(AuthResultCode.INVALID_PASSWORD);
        }
        if (!StringUtils.hasText(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_REQUIRED);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}