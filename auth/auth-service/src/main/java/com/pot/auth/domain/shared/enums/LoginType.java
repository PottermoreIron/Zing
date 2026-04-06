package com.pot.auth.domain.shared.enums;

import lombok.Getter;

@Getter
public enum LoginType {

        USERNAME_PASSWORD("username_password", "用户名密码登录"),

        EMAIL_PASSWORD("email_password", "邮箱密码登录"),

        EMAIL_CODE("email_code", "邮箱验证码登录"),

        PHONE_CODE("phone_code", "手机号验证码登录"),

        OAUTH2("oauth2", "OAuth2登录"),

        WECHAT("wechat", "微信登录");

    private final String code;
    private final String description;

    LoginType(String code, String description) {
        this.code = code;
        this.description = description;
    }

        public static LoginType fromCode(String code) {
        for (LoginType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的登录类型: " + code);
    }
}
