package com.pot.auth.domain.shared.enums;

import lombok.Getter;

/**
 * 一键认证类型枚举
 *
 * <p>
 * 定义一键认证（自动注册/登录）支持的所有认证方式
 * <p>
 * 与 {@link LoginType} 和 {@link RegisterType} 的区别：
 * <ul>
 * <li>LoginType: 传统登录，要求用户已注册</li>
 * <li>RegisterType: 传统注册，要求用户未注册</li>
 * <li>AuthType: 一键认证，自动判断注册/登录，无需用户选择</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Getter
public enum AuthType {

    /**
     * 用户名密码认证
     */
    USERNAME_PASSWORD("username_password", "用户名密码认证"),

    /**
     * 手机号密码认证
     */
    PHONE_PASSWORD("phone_password", "手机号密码认证"),

    /**
     * 手机号验证码认证
     */
    PHONE_CODE("phone_code", "手机号验证码认证"),

    /**
     * 邮箱密码认证
     */
    EMAIL_PASSWORD("email_password", "邮箱密码认证"),

    /**
     * 邮箱验证码认证
     */
    EMAIL_CODE("email_code", "邮箱验证码认证"),

    /**
     * OAuth2认证（Google、GitHub等）
     */
    OAUTH2("oauth2", "OAuth2认证"),

    /**
     * 微信认证
     */
    WECHAT("wechat", "微信认证");

    private final String code;
    private final String description;

    AuthType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 从code获取枚举
     *
     * @param code 认证类型code
     * @return 认证类型枚举
     * @throws IllegalArgumentException 如果code不存在
     */
    public static AuthType fromCode(String code) {
        for (AuthType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的认证类型: " + code);
    }
}
