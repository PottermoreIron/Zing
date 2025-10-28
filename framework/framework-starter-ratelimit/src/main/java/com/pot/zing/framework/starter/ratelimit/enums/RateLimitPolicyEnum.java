package com.pot.zing.framework.starter.ratelimit.enums;

import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/10/18 22:00
 * @description: 自定义限流策略枚举
 */
@Getter
public enum RateLimitPolicyEnum {
    /**
     * 拒绝策略
     */
    REJECT("reject", "拒绝请求"),

    /**
     * 等待策略
     */
    WAIT("wait", "等待获取令牌");

    private final String code;
    private final String description;

    RateLimitPolicyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
