package com.pot.user.service.enums;

import lombok.Getter;

import java.util.List;
import java.util.Optional;

/**
 * @author: Pot
 * @created: 2025/3/10 22:57
 * @description: 注册方式类型
 */
@Getter
public enum RegisterType {
    USERNAME_PASSWORD(1, "username_password", "username", "password"),
    PHONE_PASSWORD(2, "phone_password", "phone", "password"),
    EMAIL_PASSWORD(3, "email_password", "email", "password"),
    PHONE_CODE(4, "phone_code", "phone", "code"),
    EMAIL_CODE(5, "email_code", "email", "code"),
    THIRD_PARTY(6, "third_party", "provider", "access_token");
    
    private final int code;
    private final String type;
    private final List<String> fields;

    RegisterType(int code, String type, String... fields) {
        this.code = code;
        this.type = type;
        this.fields = List.of(fields);
    }

    public static Optional<RegisterType> getByCode(int code) {
        for (RegisterType value : RegisterType.values()) {
            if (value.code == code) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static Optional<RegisterType> getByType(String type) {
        for (RegisterType value : RegisterType.values()) {
            if (value.type.equals(type)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

}
