package com.pot.auth.domain.shared.enums;

import lombok.Getter;

@Getter
public enum AuthType {

        USERNAME_PASSWORD("username_password", "用户名密码认证"),

        PHONE_PASSWORD("phone_password", "手机号密码认证"),

        PHONE_CODE("phone_code", "手机号验证码认证"),

        EMAIL_PASSWORD("email_password", "邮箱密码认证"),

        EMAIL_CODE("email_code", "邮箱验证码认证"),

        OAUTH2("oauth2", "OAuth2认证"),

        WECHAT("wechat", "微信认证");

    private final String code;
    private final String description;

    AuthType(String code, String description) {
        this.code = code;
        this.description = description;
    }

        public static AuthType fromCode(String code) {
        for (AuthType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的认证类型: " + code);
    }
}
