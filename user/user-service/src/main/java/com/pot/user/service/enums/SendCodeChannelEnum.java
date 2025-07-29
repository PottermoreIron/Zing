package com.pot.user.service.enums;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: Pot
 * @created: 2025/3/27 23:43
 * @description: 验证码渠道枚举
 */
@Getter
public enum SendCodeChannelEnum {
    PHONE(1, "phone"),
    EMAIL(2, "email"),
    WECHAT(3, "wechat");
    private final int code;
    private final String name;

    SendCodeChannelEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    private static final Map<Integer, SendCodeChannelEnum> VERIFICATION_CODE_TYPE_MAP;

    static {
        VERIFICATION_CODE_TYPE_MAP = Stream.of(values()).collect(Collectors.toMap(SendCodeChannelEnum::getCode, e -> e));
    }

    public static SendCodeChannelEnum getByCode(int code) {
        return VERIFICATION_CODE_TYPE_MAP.getOrDefault(code, null);
    }

}
