package com.pot.auth.domain.shared.enums;

import lombok.Getter;

/**
 * 登录类型枚举
 *
 * <p>
 * 定义系统支持的所有登录方式
 *
 * @author pot
 * @since 2025-11-18
 */
@Getter
public enum LoginType {

    /**
     * 用户名密码登录
     */
    USERNAME_PASSWORD("username_password", "用户名密码登录"),

    /**
     * 邮箱密码登录
     */
    EMAIL_PASSWORD("email_password", "邮箱密码登录"),

    /**
     * 邮箱验证码登录
     */
    EMAIL_CODE("email_code", "邮箱验证码登录"),

    /**
     * 手机号验证码登录
     */
    PHONE_CODE("phone_code", "手机号验证码登录"),

    /**
     * OAuth2登录（Google、GitHub等）
     */
    OAUTH2("oauth2", "OAuth2登录"),

    /**
     * 微信登录
     */
    WECHAT("wechat", "微信登录");

    private final String code;
    private final String description;

    LoginType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 从code获取枚举
     */
    public static LoginType fromCode(String code) {
        for (LoginType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的登录类型: " + code);
    }
}
