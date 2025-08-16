package com.pot.common.enums.ratelimit;

/**
 * @author: Pot
 * @created: 2025/3/30 16:08
 * @description: 限流类型枚举
 */
public enum RateLimitMethodEnum {
    /**
     * 固定速率限流
     */
    FIXED,

    /**
     * 根据IP地址限流（扩展）
     */
    IP_BASED,

    /**
     * 根据用户ID限流（扩展）
     */
    USER_BASED
}
