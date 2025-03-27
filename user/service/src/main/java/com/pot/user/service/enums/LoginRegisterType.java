package com.pot.user.service.enums;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum LoginRegisterType {
    USERNAME_PASSWORD(1, "username", "password"),
    PHONE_PASSWORD(2, "phone", "password"),
    EMAIL_PASSWORD(3, "email", "password"),
    PHONE_CODE(4, "phone", "code"),
    EMAIL_CODE(5, "email", "code"),
    THIRD_PARTY(6, null, null);

    private final int code;
    private final String identifier;
    private final String credentials;

    private static final Map<Integer, LoginRegisterType> REGISTER_CODE_MAP;

    static {
        REGISTER_CODE_MAP = Stream.of(values()).collect(Collectors.toMap(LoginRegisterType::getCode, e -> e));
    }

    LoginRegisterType(int code, String identifier, String credentials) {
        this.code = code;
        this.identifier = identifier;
        this.credentials = credentials;
    }

    public static LoginRegisterType getByCode(int code) {
        return REGISTER_CODE_MAP.getOrDefault(code, null);
    }

}

