package com.pot.member.service.enums;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: Pot
 * @created: 2025/4/6 15:00
 * @description: Oauth2枚举
 */
@Getter
public enum OAuth2Enum {
    GITHUB(1, "github"),
    GOOGLE(2, "google"),
    WECHAT(3, "wechat");

    private final Integer code;
    private final String name;
    private static final Map<String, OAuth2Enum> OAUTH2_CODE_MAP;

    static {
        OAUTH2_CODE_MAP = Stream.of(values()).collect(Collectors.toMap(OAuth2Enum::getName, e -> e));
    }

    OAuth2Enum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static OAuth2Enum getByName(String name) {
        return OAUTH2_CODE_MAP.getOrDefault(name, null);
    }

}
