package com.pot.member.service.domain.model.member;

public record MemberId(Long value) {

    public MemberId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Member ID must not be null and must be a positive number");
        }
    }

    public static MemberId of(Long value) {
        return new MemberId(value);
    }
}
