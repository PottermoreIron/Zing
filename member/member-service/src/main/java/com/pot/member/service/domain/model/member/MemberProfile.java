package com.pot.member.service.domain.model.member;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberProfile {

    private final String nickname;
    private final String firstName;
    private final String lastName;
    private final Integer gender;
    private final String birthDate;
    private final String bio;
    private final String countryCode;
    private final String region;
    private final String city;
    private final String timezone;
    private final String locale;

        public static MemberProfile empty() {
        return builder().build();
    }

        public MemberProfile withNickname(String nickname) {
        return toBuilder()
                .nickname(nickname)
                .build();
    }
}
