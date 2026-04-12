package com.pot.auth.domain.shared.enums;

import lombok.Getter;

@Getter
public enum AuthType {

        USERNAME_PASSWORD("username_password", "Username-password authentication"),

        PHONE_PASSWORD("phone_password", "Phone-password authentication"),

        PHONE_CODE("phone_code", "Phone verification code authentication"),

        EMAIL_PASSWORD("email_password", "Email-password authentication"),

        EMAIL_CODE("email_code", "Email verification code authentication"),

        OAUTH2("oauth2", "OAuth2 authentication"),

        WECHAT("wechat", "WeChat authentication");

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
        throw new IllegalArgumentException("Unknown authentication type: " + code);
    }
}
