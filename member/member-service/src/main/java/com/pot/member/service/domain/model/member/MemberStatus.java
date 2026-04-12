package com.pot.member.service.domain.model.member;

public enum MemberStatus {
        ACTIVE(0, "Active"),

        DISABLED(1, "Disabled"),

        LOCKED(2, "Locked");

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
        throw new IllegalArgumentException("Unknown member status code: " + code);
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
