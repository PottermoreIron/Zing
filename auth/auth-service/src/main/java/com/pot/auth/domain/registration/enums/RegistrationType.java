package com.pot.auth.domain.registration.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/11/17 23:14
 * @description: 注册方式枚举
 */
@Getter
@RequiredArgsConstructor
public enum RegistrationType {
    /**
     * 用户名-密码
     */
    USERNAME_PASSWORD("username_password", "用户名密码注册", false),

    /**
     * 手机号-密码
     */
    PHONE_PASSWORD("phone_password", "手机号密码注册", true),

    /**
     * 邮箱-密码
     */
    EMAIL_PASSWORD("email_password", "邮箱密码注册", true),

    /**
     * 手机号-验证码
     */
    PHONE_CODE("phone_code", "手机号验证码注册", true),

    /**
     * 邮箱-验证码
     */
    EMAIL_CODE("email_code", "邮箱验证码注册", true);

    private final String code;
    private final String description;
    private final boolean requiresVerification;
}
