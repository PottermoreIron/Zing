package com.pot.member.service.application.query;

import lombok.Getter;

@Getter
public final class GetMemberPermissionsQuery {

    private final Long memberId;

    private GetMemberPermissionsQuery(Long memberId) {
        this.memberId = memberId;
    }

    public static GetMemberPermissionsQuery ofMemberId(Long memberId) {
        return new GetMemberPermissionsQuery(memberId);
    }
}
