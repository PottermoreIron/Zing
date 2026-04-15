package com.pot.auth.application.validation.handler;

import com.pot.auth.application.command.LoginCommand;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.application.validation.ValidationHandler;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.zing.framework.common.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationParameterValidator implements ValidationHandler<AuthenticationContext> {

    @Override
    public void validate(AuthenticationContext context) {
        LoginCommand request = context.request();
        log.debug("[ParamValidator] Validating login request — type={}", request.loginType());

        switch (request.loginType()) {
            case USERNAME_PASSWORD -> validateNicknamePassword(request);
            case EMAIL_PASSWORD -> validateEmailPassword(request);
            case EMAIL_CODE -> validateEmailCode(request);
            case PHONE_CODE -> validatePhoneCode(request);
            case PHONE_PASSWORD -> validatePhonePassword(request);
            default -> throw new DomainException(AuthResultCode.UNSUPPORTED_LOGIN_TYPE);
        }
    }

    private void validateNicknamePassword(LoginCommand request) {
        if (!ValidationUtils.isValidNickname(request.nickname())) {
            throw new DomainException(AuthResultCode.INVALID_USERNAME);
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException(AuthResultCode.INVALID_PASSWORD);
        }
    }

    private void validateEmailPassword(LoginCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException(AuthResultCode.INVALID_EMAIL);
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException(AuthResultCode.INVALID_PASSWORD);
        }
    }

    private void validateEmailCode(LoginCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException(AuthResultCode.INVALID_EMAIL);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validatePhoneCode(LoginCommand request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException(AuthResultCode.INVALID_PHONE);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validatePhonePassword(LoginCommand request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException(AuthResultCode.INVALID_PHONE);
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException(AuthResultCode.INVALID_PASSWORD);
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}