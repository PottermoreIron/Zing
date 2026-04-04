package com.pot.auth.application.validation.handler;

import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.validation.ValidationHandler;
import com.pot.auth.interfaces.dto.onestop.EmailCodeAuthRequest;
import com.pot.auth.interfaces.dto.onestop.EmailPasswordAuthRequest;
import com.pot.auth.interfaces.dto.onestop.OAuth2AuthRequest;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
import com.pot.auth.interfaces.dto.onestop.PhoneCodeAuthRequest;
import com.pot.auth.interfaces.dto.onestop.PhonePasswordAuthRequest;
import com.pot.auth.interfaces.dto.onestop.UsernamePasswordAuthRequest;
import com.pot.auth.interfaces.dto.onestop.WeChatAuthRequest;
import com.pot.zing.framework.common.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 一键认证参数校验器。
 */
@Slf4j
public class OneStopAuthenticationParameterValidator implements ValidationHandler<OneStopAuthContext> {

    @Override
    public void validate(OneStopAuthContext context) {
        OneStopAuthRequest request = context.request();
        log.debug("[参数校验] 开始校验一键认证请求: type={}", request.authType());

        switch (request.authType()) {
            case USERNAME_PASSWORD -> validateUsernamePassword((UsernamePasswordAuthRequest) request);
            case PHONE_PASSWORD -> validatePhonePassword((PhonePasswordAuthRequest) request);
            case PHONE_CODE -> validatePhoneCode((PhoneCodeAuthRequest) request);
            case EMAIL_PASSWORD -> validateEmailPassword((EmailPasswordAuthRequest) request);
            case EMAIL_CODE -> validateEmailCode((EmailCodeAuthRequest) request);
            case OAUTH2 -> validateOAuth2((OAuth2AuthRequest) request);
            case WECHAT -> validateWeChat((WeChatAuthRequest) request);
            default -> throw new DomainException(AuthResultCode.UNSUPPORTED_AUTHENTICATION_TYPE);
        }
    }

    private void validateUsernamePassword(UsernamePasswordAuthRequest request) {
        if (!ValidationUtils.isValidNickname(request.username())) {
            throw new DomainException("用户名不合法");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validatePhonePassword(PhonePasswordAuthRequest request) {
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

    private void validatePhoneCode(PhoneCodeAuthRequest request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException(AuthResultCode.INVALID_PHONE);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validateEmailPassword(EmailPasswordAuthRequest request) {
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

    private void validateEmailCode(EmailCodeAuthRequest request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException(AuthResultCode.INVALID_EMAIL);
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException(AuthResultCode.CODE_FORMAT_INVALID);
        }
    }

    private void validateOAuth2(OAuth2AuthRequest request) {
        if (request.provider() == null || !StringUtils.hasText(request.code())) {
            throw new DomainException(AuthResultCode.INVALID_PARAMETER);
        }
    }

    private void validateWeChat(WeChatAuthRequest request) {
        if (!StringUtils.hasText(request.code())) {
            throw new DomainException(AuthResultCode.INVALID_PARAMETER);
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}