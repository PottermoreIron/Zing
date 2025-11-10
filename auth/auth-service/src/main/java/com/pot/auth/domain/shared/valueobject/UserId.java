package com.pot.auth.domain.shared.valueobject;

import lombok.Builder;

/**
 * 用户ID值对象
 *
 * <p>提供类型安全，避免Long类型的ID混淆
 * <p>使用分布式ID生成器生成（Snowflake算法）
 *
 * @author pot
 * @since 1.0.0
 */
@Builder
public record UserId(Long value) {

    public UserId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("UserId必须是正整数");
        }
    }

    /**
     * 从Long创建
     */
    public static UserId of(Long value) {
        return new UserId(value);
    }

    /**
     * 从String创建
     */
    public static UserId of(String value) {
        try {
            return new UserId(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的UserId格式: " + value, e);
        }
    }
}

