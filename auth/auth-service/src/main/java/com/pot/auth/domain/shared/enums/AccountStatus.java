package com.pot.auth.domain.shared.enums;

import lombok.Getter;

/**
 * 用户账户状态枚举
 *
 * <p>
 * Auth模块的用户状态定义，用于认证和授权场景
 * <p>
 * 设计原则：
 * <ul>
 * <li>独立于具体的User模块实现（Member/Admin等）</li>
 * <li>使用字符串code进行适配，支持多种User模块</li>
 * <li>提供灵活的状态匹配策略</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Getter
public enum AccountStatus {
    /**
     * 活跃 - 可正常登录
     */
    ACTIVE("ACTIVE", "活跃", true),

    /**
     * 非活跃 - 需要激活才能登录
     */
    INACTIVE("INACTIVE", "非活跃", false),

    /**
     * 锁定/暂停 - 被管理员暂停或因安全原因锁定
     */
    LOCKED("LOCKED", "锁定", false),

    /**
     * 待审核 - 等待管理员审核
     */
    PENDING("PENDING", "待审核", false),

    /**
     * 已删除 - 软删除状态
     */
    DELETED("DELETED", "已删除", false),

    /**
     * 未知状态 - 兜底状态
     */
    UNKNOWN("UNKNOWN", "未知", false);

    private final String code;
    private final String description;
    private final boolean canLogin;

    AccountStatus(String code, String description, boolean canLogin) {
        this.code = code;
        this.description = description;
        this.canLogin = canLogin;
    }

    /**
     * 从字符串code解析状态
     *
     * <p>
     * 支持多种格式：
     * <ul>
     * <li>大写: ACTIVE, INACTIVE</li>
     * <li>小写: active, inactive</li>
     * <li>数字映射: 1=ACTIVE, 0=INACTIVE</li>
     * </ul>
     *
     * @param statusCode 状态码（可能来自不同的User模块）
     * @return 账户状态枚举
     */
    public static AccountStatus fromCode(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return UNKNOWN;
        }

        // 标准化处理
        String normalizedCode = statusCode.trim().toUpperCase();

        // 直接匹配
        for (AccountStatus status : values()) {
            if (status.code.equals(normalizedCode)) {
                return status;
            }
        }

        // 别名匹配（兼容不同User模块的命名）
        return switch (normalizedCode) {
            case "SUSPENDED", "BANNED", "BLOCKED" -> LOCKED;
            case "UNVERIFIED", "UNACTIVATED" -> INACTIVE;
            case "NORMAL", "ENABLED", "VALID" -> ACTIVE;
            case "REMOVED", "CANCELED" -> DELETED;
            case "REVIEWING", "AUDITING" -> PENDING;
            // 数字映射（某些系统使用数字状态）
            case "1", "TRUE" -> ACTIVE;
            case "0", "FALSE" -> INACTIVE;
            default -> UNKNOWN;
        };
    }

    /**
     * 是否可以登录
     */
    public boolean isLoginAllowed() {
        return canLogin;
    }

    /**
     * 获取不能登录的原因描述
     */
    public String getLoginDeniedReason() {
        return switch (this) {
            case LOCKED -> "账户已被锁定，请联系管理员";
            case INACTIVE -> "账户未激活，请先激活账户";
            case DELETED -> "账户已被删除";
            case PENDING -> "账户正在审核中，请耐心等待";
            case UNKNOWN -> "账户状态异常，请联系客服";
            default -> "账户不可用";
        };
    }
}
