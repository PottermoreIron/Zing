package com.pot.member.facade.dto;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record MemberProfileDTO(
        Long memberId,
        String nickname,
        String firstName,
        String lastName,
        Integer gender,
        String birthDate,
        String avatarUrl,
        String bio,
        String countryCode,
        String region,
        String city,
        String timezone,
        String locale) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
