package com.pot.zing.framework.starter.ratelimit.enums;

import lombok.Getter;

/**
 * Supported rate-limit policies.
 */
@Getter
public enum RateLimitPolicyEnum {
    REJECT("reject", "拒绝请求"),

    WAIT("wait", "等待获取令牌");

    private final String code;
    private final String description;

    RateLimitPolicyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
