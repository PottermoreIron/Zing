package com.pot.auth.domain.shared.enums;

import lombok.Getter;

/**
 * 注册类型枚举
 *
 * <p>定义系统支持的所有注册方式
 *
 * @author yecao
 * @since 2025-11-18
 */
@Getter
public enum RegisterType {

    /**
     * 用户名密码注册
     */
    USERNAME_PASSWORD("username_password", "用户名密码注册"),

    /**
     * 邮箱密码注册
     */
    EMAIL_PASSWORD("email_password", "邮箱密码注册"),

    /**
     * 手机号密码注册
     */
    PHONE_PASSWORD("phone_password", "手机号密码注册"),

    /**
     * 邮箱验证码注册
     */
    EMAIL_CODE("email_code", "邮箱验证码注册"),

    /**
     * 手机号验证码注册
     */
    PHONE_CODE("phone_code", "手机号验证码注册"),

    /**
     * OAuth2注册（Google、GitHub等）
     */
    OAUTH2("oauth2", "OAuth2注册"),

    /**
     * 微信注册
     */
    WECHAT("wechat", "微信注册");

    private final String code;
    private final String description;

    RegisterType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 从code获取枚举
     */
    public static RegisterType fromCode(String code) {
        for (RegisterType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的注册类型: " + code);
    }
}