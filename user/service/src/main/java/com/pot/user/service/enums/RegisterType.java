package com.pot.user.service.enums;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum RegisterType {
    //    USERNAME_PASSWORD(1, "username_password", "username", "password"),
//    PHONE_PASSWORD(2, "phone_password", "phone", "password"),
//    EMAIL_PASSWORD(3, "email_password", "email", "password"),
    PHONE_CODE(4, "phone_code", "phone", "code");
//    EMAIL_CODE(5, "email_code", "email", "code"),
//    THIRD_PARTY(6, "third_party", "provider", "access_token");

    private final int code;
    private final String type;
    private final List<String> fields;

    private static final Map<Integer, RegisterType> REGISTER_CODE_MAP;
    private static final Map<String, RegisterType> REGISTER_TYPE_MAP;

    static {
        REGISTER_CODE_MAP = Stream.of(values()).collect(Collectors.toMap(RegisterType::getCode, e -> e));
        REGISTER_TYPE_MAP = Stream.of(values()).collect(Collectors.toMap(RegisterType::getType, e -> e));
    }

    RegisterType(int code, String type, String... fields) {
        this.code = code;
        this.type = type;
        this.fields = List.of(fields);
    }

    public static RegisterType getByCode(int code) {
        return REGISTER_CODE_MAP.getOrDefault(code, null);
    }

    public static RegisterType getByType(String type) {
        return REGISTER_TYPE_MAP.getOrDefault(type, null);
    }
}

