package com.pot.member.service.domain.model.member;

public enum MemberStatus {
        ACTIVE(0, "正常"),

        DISABLED(1, "禁用"),

        LOCKED(2, "锁定");

    private final int code;
    private final String description;

    MemberStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MemberStatus fromCode(int code) {
        for (MemberStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的会员状态代码: " + code);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
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
