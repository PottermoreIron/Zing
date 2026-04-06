package com.pot.member.service.domain.model.member;

public record MemberId(Long value) {

    public MemberId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("会员ID不能为空且必须为正数");
        }
    }

    public static MemberId of(Long value) {
        return new MemberId(value);
    }
}
