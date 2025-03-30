package com.pot.user.service.enums.ratelimit;

/**
 * @author: Pot
 * @created: 2025/3/30 16:09
 * @description: 限流策略枚举
 */
public enum LimitPolicy {
    /**
     * 拒绝策略：直接拒绝请求
     */
    REJECT,

    /**
     * 等待策略：尝试等待获取令牌
     */
    WAIT
}
