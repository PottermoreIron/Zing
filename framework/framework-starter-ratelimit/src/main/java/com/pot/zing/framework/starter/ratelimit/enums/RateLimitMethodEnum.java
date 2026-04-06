package com.pot.zing.framework.starter.ratelimit.enums;

import lombok.Getter;

/**
 * Supported rate-limit key strategies.
 */
@Getter
public enum RateLimitMethodEnum {

    FIXED("fixed", "固定限流"),

    IP_BASED("ip", "IP限流"),

    USER_BASED("user", "用户限流");

    private final String code;
    private final String description;

    RateLimitMethodEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
