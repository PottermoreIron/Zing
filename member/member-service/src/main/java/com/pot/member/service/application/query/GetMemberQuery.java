package com.pot.member.service.application.query;

import lombok.Getter;

/**
 * Query object that represents exactly one member lookup selector.
 */
@Getter
public final class GetMemberQuery {

    public enum Selector {
        MEMBER_ID,
        EMAIL,
        PHONE_NUMBER,
        NICKNAME
    }

    private final Selector selector;

    private final Long memberId;
    private final String email;
    private final String phoneNumber;
    private final String nickname;

    private GetMemberQuery(Selector selector, Long memberId, String email, String phoneNumber, String nickname) {
        this.selector = selector;
        this.memberId = memberId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
    }

    public static GetMemberQuery byMemberId(Long memberId) {
        return new GetMemberQuery(Selector.MEMBER_ID, memberId, null, null, null);
    }

    public static GetMemberQuery byEmail(String email) {
        return new GetMemberQuery(Selector.EMAIL, null, email, null, null);
    }

    public static GetMemberQuery byPhoneNumber(String phoneNumber) {
        return new GetMemberQuery(Selector.PHONE_NUMBER, null, null, phoneNumber, null);
    }

    public static GetMemberQuery byNickname(String nickname) {
        return new GetMemberQuery(Selector.NICKNAME, null, null, null, nickname);
    }
}
