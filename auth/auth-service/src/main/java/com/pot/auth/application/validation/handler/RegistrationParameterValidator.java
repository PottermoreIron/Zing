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
        log.debug("[参数校验] 开始校验注册请求: type={}", request.registerType());

        switch (request.registerType()) {
            case USERNAME_PASSWORD -> validateNicknamePassword(request);
            case EMAIL_PASSWORD -> validateEmailPassword(request);
            case EMAIL_CODE -> validateEmailCode(request);
            case PHONE_CODE -> validatePhoneCode(request);
            case OAUTH2, WECHAT -> {
            }
            default -> throw new DomainException("不支持的注册类型: " + request.registerType());
        }
    }

    private void validateNicknamePassword(RegisterCommand request) {
        if (!ValidationUtils.isValidNickname(request.nickname())) {
            throw new DomainException("昵称不合法");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validateEmailPassword(RegisterCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("邮箱格式不正确");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validateEmailCode(RegisterCommand request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("邮箱格式不正确");
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException("验证码格式不正确");
        }
    }

    private void validatePhoneCode(RegisterCommand request) {
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