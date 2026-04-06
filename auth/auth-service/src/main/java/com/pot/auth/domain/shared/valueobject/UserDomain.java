package com.pot.auth.domain.shared.valueobject;

import lombok.Getter;

@Getter
public enum UserDomain {

        MEMBER("member", "会员"),

        ADMIN("admin", "后台用户"),

        MERCHANT("merchant", "商户");

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
        throw new IllegalArgumentException("无效的用户域编码: " + code);
    }
}

