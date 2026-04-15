package com.pot.auth.domain.shared.enums;

import lombok.Getter;

@Getter
public enum RegisterType {

        USERNAME_PASSWORD("username_password", "Username-password registration"),

        EMAIL_PASSWORD("email_password", "Email-password registration"),

        EMAIL_CODE("email_code", "Email verification code registration"),

        PHONE_CODE("phone_code", "Phone verification code registration"),

        PHONE_PASSWORD("phone_password", "Phone-password registration"),

        OAUTH2("oauth2", "OAuth2 registration"),

        WECHAT("wechat", "WeChat registration");

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
        throw new IllegalArgumentException("Unknown registration type: " + code);
    }
}