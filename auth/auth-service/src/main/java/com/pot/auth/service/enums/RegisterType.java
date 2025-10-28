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
 * @created: 2025/10/12 23:15
 * @description: 注册方式枚举
 */
@Getter
@AllArgsConstructor
public enum RegisterType {
    USERNAME_PASSWORD(1, "用户名密码注册"),
    PHONE_PASSWORD(2, "手机号密码注册"),
    EMAIL_PASSWORD(3, "邮箱密码注册"),
    PHONE_CODE(4, "手机验证码注册"),
    EMAIL_CODE(5, "邮箱验证码注册");

    /**
     * code -> Enum  映射
     */
    private static final Map<Integer, RegisterType> TYPE_MAP =
            Stream.of(values()).collect(Collectors.toMap(RegisterType::getCode, e -> e));
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
    public static RegisterType fromCode(Integer code) {
        RegisterType type = TYPE_MAP.get(code);
        if (type == null) {
            throw new IllegalArgumentException("无效的发送验证码类型: " + code);
        }
        return type;
    }
}
