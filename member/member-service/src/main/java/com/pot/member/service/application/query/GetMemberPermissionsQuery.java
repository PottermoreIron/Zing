package com.pot.member.service.application.query;

public record GetMemberPermissionsQuery(Long memberId) {

    public static GetMemberPermissionsQuery ofMemberId(Long memberId) {
        return new GetMemberPermissionsQuery(memberId);
    }
}
