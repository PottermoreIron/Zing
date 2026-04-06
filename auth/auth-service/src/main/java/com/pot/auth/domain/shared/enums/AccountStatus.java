package com.pot.auth.domain.shared.enums;

import lombok.Getter;

@Getter
public enum AccountStatus {
        ACTIVE("ACTIVE", "活跃", true),

        INACTIVE("INACTIVE", "非活跃", false),

        LOCKED("LOCKED", "锁定", false),

        PENDING("PENDING", "待审核", false),

        DELETED("DELETED", "已删除", false),

        UNKNOWN("UNKNOWN", "未知", false);

    private final String code;
    private final String description;
    private final boolean canLogin;

    AccountStatus(String code, String description, boolean canLogin) {
        this.code = code;
        this.description = description;
        this.canLogin = canLogin;
    }

        public static AccountStatus fromCode(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return UNKNOWN;
        }

        String normalizedCode = statusCode.trim().toUpperCase();

        for (AccountStatus status : values()) {
            if (status.code.equals(normalizedCode)) {
                return status;
            }
        }

        return switch (normalizedCode) {
            case "SUSPENDED", "BANNED", "BLOCKED" -> LOCKED;
            case "UNVERIFIED", "UNACTIVATED" -> INACTIVE;
            case "NORMAL", "ENABLED", "VALID" -> ACTIVE;
            case "REMOVED", "CANCELED" -> DELETED;
            case "REVIEWING", "AUDITING" -> PENDING;
            case "1", "TRUE" -> ACTIVE;
            case "0", "FALSE" -> INACTIVE;
            default -> UNKNOWN;
        };
    }

        public boolean isLoginAllowed() {
        return canLogin;
    }

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
