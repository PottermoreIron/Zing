package com.pot.zing.framework.starter.ratelimit.enums;

import lombok.Getter;

/**
 * Supported rate-limit key strategies.
 */
@Getter
public enum RateLimitMethodEnum {

    FIXED("fixed", "Fixed rate"),

    IP_BASED("ip", "IP-based"),

    USER_BASED("user", "User-based");

    private final String code;
    private final String description;

    RateLimitMethodEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
