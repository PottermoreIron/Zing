package com.pot.auth.domain.validation.handler;

import com.pot.auth.domain.context.RegistrationContext;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.validation.ValidationHandler;
import com.pot.auth.interfaces.dto.register.*;
import com.pot.zing.framework.common.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 注册参数校验处理器
 *
 * <p>
 * 对注册请求进行基础参数校验
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class RegistrationParameterValidator implements ValidationHandler<RegistrationContext> {

    @Override
    public void validate(RegistrationContext context) {
        RegisterRequest request = context.request();

        log.debug("[参数校验] 开始校验注册请求: type={}", request.registerType());

        switch (request.registerType()) {
            case USERNAME_PASSWORD -> validateUsernamePassword((UsernamePasswordRegisterRequest) request);
            case EMAIL_PASSWORD -> validateEmailPassword((EmailPasswordRegisterRequest) request);
            case EMAIL_CODE -> validateEmailCode((EmailCodeRegisterRequest) request);
            case PHONE_CODE -> validatePhoneCode((PhoneCodeRegisterRequest) request);
            case OAUTH2, WECHAT -> {
                // OAuth2 和 WeChat 由其他策略处理
            }
            default -> throw new DomainException("不支持的注册类型: " + request.registerType());
        }
    }

    private void validateUsernamePassword(UsernamePasswordRegisterRequest request) {
        if (!ValidationUtils.isValidNickname(request.username())) {
            throw new DomainException("用户名不合法");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validateEmailPassword(EmailPasswordRegisterRequest request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("邮箱格式不正确");
        }
        if (!ValidationUtils.isValidPassword(request.password())) {
            throw new DomainException("密码不合法");
        }
    }

    private void validateEmailCode(EmailCodeRegisterRequest request) {
        if (!ValidationUtils.isValidEmail(request.email())) {
            throw new DomainException("邮箱格式不正确");
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException("验证码格式不正确");
        }
    }

    private void validatePhoneCode(PhoneCodeRegisterRequest request) {
        if (!ValidationUtils.isValidPhone(request.phone())) {
            throw new DomainException("手机号格式不正确");
        }
        if (!ValidationUtils.isValidVerificationCode(request.verificationCode())) {
            throw new DomainException("验证码格式不正确");
        }
    }

    @Override
    public int getOrder() {
        return 10; // 参数校验优先级最高
    }
}
