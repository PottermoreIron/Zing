package com.pot.auth.service.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: Pot
 * @created: 2025/10/12 23:11
 * @description: 登录方式枚举
 */
@Getter
@AllArgsConstructor
public enum LoginType {
    USERNAME_PASSWORD(1, "用户名密码登录"),
    PHONE_PASSWORD(2, "手机号密码登录"),
    EMAIL_PASSWORD(3, "邮箱密码登录"),
    PHONE_CODE(4, "手机验证码登录"),
    EMAIL_CODE(5, "邮箱验证码登录"),
    OAUTH2_GITHUB(6, "GitHub OAuth2登录"),
    OAUTH2_GOOGLE(7, "Google OAuth2登录"),
    OAUTH2_WECHAT(8, "微信 OAuth2登录"),
    OAUTH2_FACEBOOK(9, "Facebook OAuth2登录"),
    OAUTH2_TWITTER(10, "Twitter OAuth2登录");

    /**
     * code -> Enum  映射
     */
    private static final Map<Integer, LoginType> TYPE_MAP =
            Stream.of(values()).collect(Collectors.toMap(LoginType::getCode, e -> e));
    /**
     * code
     */
    @JsonValue
    private final Integer code;
    /**
     * 描述
     */
    private final String description;

    @JsonCreator
    public static LoginType fromCode(Integer code) {
        LoginType type = TYPE_MAP.get(code);
        if (type == null) {
            throw new IllegalArgumentException("无效的发送验证码类型: " + code);
        }
        return type;
    }
}
