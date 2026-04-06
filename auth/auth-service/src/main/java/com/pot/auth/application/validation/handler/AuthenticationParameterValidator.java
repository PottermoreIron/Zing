package com.pot.auth.application.validation.handler;

import com.pot.auth.application.command.LoginCommand;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.application.validation.ValidationHandler;
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
        log.debug("[参数校验] 开始校验登录请求: type={}", request.loginType());

        switch (request.loginType()) {
            case USERNAME_PASSWORD -> validateNicknamePassword(request);
            case EMAIL_PASSWORD -> validateEmailPassword(request);
            case EMAIL_CODE -> validateEmailCode(request);
            case PHONE_CODE -> validatePhoneCode(request);
            default -> throw new DomainException("不支持的登录类型: " + request.loginType());
        }
    }

    private void validateNicknamePassword(LoginCommand request) {
        if (!ValidationUtils.isValidNickname(request.nickname())) {
            throw new DomainException("昵称不合法");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validateEmailPassword(LoginCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("邮箱格式不正确");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validateEmailCode(LoginCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("邮箱格式不正确");
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException("验证码格式不正确");
        }
    }

    private void validatePhoneCode(LoginCommand request) {
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