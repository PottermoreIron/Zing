package com.pot.auth.domain.shared.enums;

import lombok.Getter;

@Getter
public enum AccountStatus {
        ACTIVE("ACTIVE", "Active", true),

        INACTIVE("INACTIVE", "Inactive", false),

        LOCKED("LOCKED", "Locked", false),

        PENDING("PENDING", "Pending", false),

        DELETED("DELETED", "Deleted", false),

        UNKNOWN("UNKNOWN", "Unknown", false);

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
            case LOCKED -> "Account is locked, please contact your administrator";
            case INACTIVE -> "Account is inactive, please activate your account";
            case DELETED -> "Account has been deleted";
            case PENDING -> "Account is pending review, please wait";
            case UNKNOWN -> "Account status is abnormal, please contact support";
            default -> "Account is unavailable";
        };
    }
}
