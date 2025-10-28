package com.pot.zing.framework.starter.ratelimit.enums;

import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/10/18 22:01
 * @description: 自定义限流方法枚举
 */
@Getter
public enum RateLimitMethodEnum {
    /**
     * 固定速率限流
     */
    FIXED("fixed", "固定限流"),

    /**
     * 基于IP限流
     */
    IP_BASED("ip", "IP限流"),

    /**
     * 基于用户限流
     */
    USER_BASED("user", "用户限流");

    private final String code;
    private final String description;

    RateLimitMethodEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
