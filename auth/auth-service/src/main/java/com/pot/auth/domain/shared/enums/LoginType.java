package com.pot.auth.domain.shared.enums;

import lombok.Getter;

@Getter
public enum LoginType {

        USERNAME_PASSWORD("username_password", "Username-password login"),

        EMAIL_PASSWORD("email_password", "Email-password login"),

        EMAIL_CODE("email_code", "Email verification code login"),

        PHONE_CODE("phone_code", "Phone verification code login"),

        OAUTH2("oauth2", "OAuth2 login"),

        WECHAT("wechat", "WeChat login");

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
        throw new IllegalArgumentException("Unknown login type: " + code);
    }
}
