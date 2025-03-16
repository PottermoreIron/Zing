package com.pot.user.service.enums;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum RegisterType {
    USERNAME_PASSWORD(1, "username_password"),
    PHONE_PASSWORD(2, "phone_password"),
    EMAIL_PASSWORD(3, "email_password"),
    PHONE_CODE(4, "phone_code"),
    EMAIL_CODE(5, "email_code"),
    THIRD_PARTY(6, "third_party");

    private final int code;
    private final String type;

    private static final Map<Integer, RegisterType> REGISTER_CODE_MAP;
    private static final Map<String, RegisterType> REGISTER_TYPE_MAP;

    static {
        REGISTER_CODE_MAP = Stream.of(values()).collect(Collectors.toMap(RegisterType::getCode, e -> e));
        REGISTER_TYPE_MAP = Stream.of(values()).collect(Collectors.toMap(RegisterType::getType, e -> e));
    }

    RegisterType(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static RegisterType getByCode(int code) {
        return REGISTER_CODE_MAP.getOrDefault(code, null);
    }

    public static RegisterType getByType(String type) {
        return REGISTER_TYPE_MAP.getOrDefault(type, null);
    }
}

