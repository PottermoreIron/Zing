package com.pot.auth.domain.registration.entity;

import com.pot.auth.domain.registration.valueobject.RegistrationType;
import com.pot.auth.domain.shared.valueobject.*;
import lombok.Builder;

import java.util.Map;

/**
 * 注册请求聚合根
 *
 * <p>封装用户注册的完整信息和业务规则
 *
 * @author yecao
 * @since 2025-11-10
 */
@Builder
public record RegistrationRequest(RegistrationType registrationType,
                                  UserDomain userDomain,
                                  String username,
                                  Email email,
                                  Phone phone,
                                  Password password,
                                  VerificationCode verificationCode,
                                  LoginContext loginContext,
                                  Map<String, Object> extendAttributes) {

    /**
     * 验证注册请求的业务规则
     */
    private void validate() {
        // 用户域不能为空
        if (userDomain == null) {
            throw new IllegalArgumentException("用户域不能为空");
        }

        // 用户名不能为空
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // 根据注册类型验证必填字段
        switch (registrationType) {
            case EMAIL -> {
                if (email == null) {
                    throw new IllegalArgumentException("邮箱注册必须提供邮箱");
                }
                if (password == null) {
                    throw new IllegalArgumentException("邮箱注册必须提供密码");
                }
                if (verificationCode == null) {
                    throw new IllegalArgumentException("邮箱注册必须提供验证码");
                }
            }
            case PHONE -> {
                if (phone == null) {
                    throw new IllegalArgumentException("手机号注册必须提供手机号");
                }
                if (password == null) {
                    throw new IllegalArgumentException("手机号注册必须提供密码");
                }
                if (verificationCode == null) {
                    throw new IllegalArgumentException("手机号注册必须提供验证码");
                }
            }
            case OAUTH2, WECHAT -> {
                // OAuth2和微信注册不需要密码和验证码
                // 由专门的服务处理
            }
        }
    }

    /**
     * 获取注册标识符（邮箱或手机号）
     */
    public String getIdentifier() {
        return switch (registrationType) {
            case EMAIL -> email != null ? email.value() : null;
            case PHONE -> phone != null ? phone.value() : null;
            default -> null;
        };
    }

    /**
     * 是否需要验证码验证
     */
    public boolean requiresCodeVerification() {
        return registrationType == RegistrationType.EMAIL
                || registrationType == RegistrationType.PHONE;
    }
}

