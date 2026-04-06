package com.pot.auth.application.validation.handler;

import com.pot.auth.application.command.OneStopAuthCommand;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.application.validation.ValidationHandler;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.zing.framework.common.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 一键认证参数校验器。
 */
@Slf4j
@Component
public class OneStopAuthenticationParameterValidator implements ValidationHandler<OneStopAuthContext> {

    @Override
    public void validate(OneStopAuthContext context) {
        OneStopAuthCommand request = context.request();
        log.debug("[参数校验] 开始校验一键认证请求: type={}", request.authType());

        switch (request.authType()) {
            case USERNAME_PASSWORD -> validateNicknamePassword(request);
            case PHONE_PASSWORD -> validatePhonePassword(request);
            case PHONE_CODE -> validatePhoneCode(request);
            case EMAIL_PASSWORD -> validateEmailPassword(request);
            case EMAIL_CODE -> validateEmailCode(request);
            case OAUTH2 -> validateOAuth2(request);
            case WECHAT -> validateWeChat(request);
            default -> throw new DomainException(AuthResultCode.UNSUPPORTED_AUTHENTICATION_TYPE);
        }
    }

    private void validateNicknamePassword(OneStopAuthCommand request) {
        if (!ValidationUtils.isValidNickname(request.nickname())) {
            throw new DomainException("昵称不合法");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validatePhonePassword(OneStopAuthCommand request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException(AuthResultCode.INVALID_PHONE);
        }
        if (!StringUtils.hasText(request.password()) && !StringUtils.hasText(request.verificationCode())) {
            throw new DomainException("手机号一键认证至少需要密码或验证码之一");
        }
        if (StringUtils.hasText(request.password()) && !ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
        if (StringUtils.hasText(request.verificationCode())
                && !ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validatePhoneCode(OneStopAuthCommand request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException(AuthResultCode.INVALID_PHONE);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validateEmailPassword(OneStopAuthCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException(AuthResultCode.INVALID_EMAIL);
        }
        if (!StringUtils.hasText(request.password()) && !StringUtils.hasText(request.verificationCode())) {
            throw new DomainException("邮箱一键认证至少需要密码或验证码之一");
        }
        if (StringUtils.hasText(request.password()) && !ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
        if (StringUtils.hasText(request.verificationCode())
                && !ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validateEmailCode(OneStopAuthCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException(AuthResultCode.INVALID_EMAIL);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validateOAuth2(OneStopAuthCommand request) {
        if (!StringUtils.hasText(request.oauth2ProviderCode()) || !StringUtils.hasText(request.code())) {
            throw new DomainException(AuthResultCode.INVALID_PARAMETER);
        }
    }

    private void validateWeChat(OneStopAuthCommand request) {
        if (!StringUtils.hasText(request.code())) {
            throw new DomainException(AuthResultCode.INVALID_PARAMETER);
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}