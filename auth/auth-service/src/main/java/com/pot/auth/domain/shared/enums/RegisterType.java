package com.pot.auth.domain.shared.enums;

import lombok.Getter;

@Getter
public enum RegisterType {

        USERNAME_PASSWORD("username_password", "用户名密码注册"),

        EMAIL_PASSWORD("email_password", "邮箱密码注册"),

        EMAIL_CODE("email_code", "邮箱验证码注册"),

        PHONE_CODE("phone_code", "手机号验证码注册"),

        OAUTH2("oauth2", "OAuth2注册"),

        WECHAT("wechat", "微信注册");

    private final String code;
    private final String description;

    RegisterType(String code, String description) {
        this.code = code;
        this.description = description;
    }

        public static RegisterType fromCode(String code) {
        for (RegisterType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的注册类型: " + code);
    }
}