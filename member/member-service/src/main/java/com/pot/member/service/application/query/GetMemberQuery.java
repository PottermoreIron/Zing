package com.pot.member.service.application.query;

import java.util.Objects;

/**
 * Query object that represents exactly one member lookup selector.
 */
public sealed interface GetMemberQuery permits GetMemberQuery.ByMemberId,
        GetMemberQuery.ByEmail,
        GetMemberQuery.ByPhoneNumber,
        GetMemberQuery.ByNickname {

    static GetMemberQuery byMemberId(Long memberId) {
        return new ByMemberId(memberId);
    }

    static GetMemberQuery byEmail(String email) {
        return new ByEmail(email);
    }

    static GetMemberQuery byPhoneNumber(String phoneNumber) {
        return new ByPhoneNumber(phoneNumber);
    }

    static GetMemberQuery byNickname(String nickname) {
        return new ByNickname(nickname);
    }

    record ByMemberId(Long memberId) implements GetMemberQuery {

        public ByMemberId {
            Objects.requireNonNull(memberId, "memberId must not be null");
        }
    }

    record ByEmail(String email) implements GetMemberQuery {

        public ByEmail {
            Objects.requireNonNull(email, "email must not be null");
        }
    }

    record ByPhoneNumber(String phoneNumber) implements GetMemberQuery {

        public ByPhoneNumber {
            Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        }
    }

    record ByNickname(String nickname) implements GetMemberQuery {

        public ByNickname {
            Objects.requireNonNull(nickname, "nickname must not be null");
        }
    }
}
