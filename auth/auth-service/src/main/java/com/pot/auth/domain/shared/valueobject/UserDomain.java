package com.pot.auth.domain.shared.valueobject;

import lombok.Getter;

@Getter
public enum UserDomain {

        MEMBER("member", "Member"),

        ADMIN("admin", "Admin"),

        MERCHANT("merchant", "Merchant");

    private final String code;
    private final String description;

    UserDomain(String code, String description) {
        this.code = code;
        this.description = description;
    }

        public static UserDomain fromCode(String code) {
        for (UserDomain domain : UserDomain.values()) {
            if (domain.getCode().equalsIgnoreCase(code)) {
                return domain;
            }
        }
        throw new IllegalArgumentException("Invalid user domain code: " + code);
    }
}

