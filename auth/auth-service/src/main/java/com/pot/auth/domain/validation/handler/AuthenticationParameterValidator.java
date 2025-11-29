package com.pot.auth.domain.validation.handler;

import com.pot.auth.domain.context.AuthenticationContext;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.validation.ValidationHandler;
import com.pot.auth.interfaces.dto.auth.*;
import com.pot.zing.framework.common.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 参数校验处理器
 *
 * <p>
 * 对认证请求进行基础参数校验（Jakarta Validation 已在 Controller 完成）
 * <p>
 * 此处主要进行业务级别的参数校验：
 * <ul>
 * <li>邮箱格式校验</li>
 * <li>手机号格式校验</li>
 * <li>密码强度校验</li>
 * <li>验证码格式校验</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class AuthenticationParameterValidator implements ValidationHandler<AuthenticationContext> {

    @Override
    public void validate(AuthenticationContext context) {
        // requst已经在controller层进行了非空校验
        LoginRequest request = context.request();
        log.debug("[参数校验] 开始校验登录请求: type={}", request.loginType());

        switch (request.loginType()) {
            case USERNAME_PASSWORD -> validateUsernamePassword((UsernamePasswordLoginRequest) request);
            case EMAIL_PASSWORD -> validateEmailPassword((EmailPasswordLoginRequest) request);
            case EMAIL_CODE -> validateEmailCode((EmailCodeLoginRequest) request);
            case PHONE_CODE -> validatePhoneCode((PhoneCodeLoginRequest) request);
            case OAUTH2, WECHAT -> {
                // OAuth2 和 WeChat 由其他策略处理
            }
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
        return 10; // 参数校验优先级最高
    }
}
