package com.pot.auth.domain.registration.valueobject;

import lombok.Getter;

/**
 * 注册类型枚举
 *
 * <p>定义系统支持的用户注册方式
 *
 * @author yecao
 * @since 2025-11-10
 */
@Getter
public enum RegistrationType {

    /**
     * 用户名注册
     */
    USERNAME("username", "用户名注册"),

    /**
     * 邮箱注册
     */
    EMAIL("email", "邮箱注册"),

    /**
     * 手机号注册
     */
    PHONE("phone", "手机号注册"),

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

    RegistrationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}

