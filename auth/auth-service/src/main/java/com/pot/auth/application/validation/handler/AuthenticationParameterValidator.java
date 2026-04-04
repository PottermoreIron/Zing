package com.pot.auth.application.validation.handler;

import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.validation.ValidationHandler;
import com.pot.auth.interfaces.dto.auth.EmailCodeLoginRequest;
import com.pot.auth.interfaces.dto.auth.EmailPasswordLoginRequest;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import com.pot.auth.interfaces.dto.auth.PhoneCodeLoginRequest;
import com.pot.auth.interfaces.dto.auth.UsernamePasswordLoginRequest;
import com.pot.zing.framework.common.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthenticationParameterValidator implements ValidationHandler<AuthenticationContext> {

    @Override
    public void validate(AuthenticationContext context) {
        LoginRequest request = context.request();
        log.debug("[参数校验] 开始校验登录请求: type={}", request.loginType());

        switch (request.loginType()) {
            case USERNAME_PASSWORD -> validateUsernamePassword((UsernamePasswordLoginRequest) request);
            case EMAIL_PASSWORD -> validateEmailPassword((EmailPasswordLoginRequest) request);
            case EMAIL_CODE -> validateEmailCode((EmailCodeLoginRequest) request);
            case PHONE_CODE -> validatePhoneCode((PhoneCodeLoginRequest) request);
            default -> throw new DomainException("不支持的登录类型: " + request.loginType());
        }
    }

    private void validateUsernamePassword(UsernamePasswordLoginRequest request) {
        if (!ValidationUtils.isValidNickname(request.username())) {
            throw new DomainException("用户名不合法");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validateEmailPassword(EmailPasswordLoginRequest request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("邮箱格式不正确");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validateEmailCode(EmailCodeLoginRequest request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("邮箱格式不正确");
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException("验证码格式不正确");
        }
    }

    private void validatePhoneCode(PhoneCodeLoginRequest request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException("手机号格式不正确");
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException("验证码格式不正确");
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}