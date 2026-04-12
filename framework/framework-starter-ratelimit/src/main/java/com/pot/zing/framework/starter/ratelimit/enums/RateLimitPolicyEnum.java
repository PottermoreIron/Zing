package com.pot.zing.framework.starter.ratelimit.enums;

import lombok.Getter;

/**
 * Supported rate-limit policies.
 */
@Getter
public enum RateLimitPolicyEnum {
    REJECT("reject", "Reject request"),

    WAIT("wait", "Wait for token acquisition");

    private final String code;
    private final String description;

    RateLimitPolicyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
