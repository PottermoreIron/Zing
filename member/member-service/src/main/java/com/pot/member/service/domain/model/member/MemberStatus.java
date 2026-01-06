package com.pot.member.service.domain.model.member;

/**
 * 会员状态枚举
 * 
 * @author Pot
 * @since 2026-01-06
 */
public enum MemberStatus {
    /**
     * 正常
     */
    ACTIVE(0, "正常"),

    /**
     * 禁用
     */
    DISABLED(1, "禁用"),

    /**
     * 锁定
     */
    LOCKED(2, "锁定");

    private final int code;
    private final String description;

    MemberStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MemberStatus fromCode(int code) {
        for (MemberStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的会员状态代码: " + code);
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isLocked() {
        return this == LOCKED;
    }

    public boolean isDisabled() {
        return this == DISABLED;
    }
}
