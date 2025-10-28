package com.pot.auth.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 一键登录/注册类型枚举
 */
@Getter
@AllArgsConstructor
public enum SignInOrRegisterType {
    /**
     * 手机验证码一键登录/注册
     */
    PHONE_CODE(1, "手机验证码一键登录"),

    /**
     * 邮箱验证码一键登录/注册
     */
    EMAIL_CODE(2, "邮箱验证码一键登录"),

    /**
     * 第三方OAuth2一键登录/注册（微信、GitHub、Google等）
     */
    OAUTH2(3, "第三方OAuth2一键登录");

    private final Integer code;
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static SignInOrRegisterType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SignInOrRegisterType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的一键登录类型: " + code);
    }
}

