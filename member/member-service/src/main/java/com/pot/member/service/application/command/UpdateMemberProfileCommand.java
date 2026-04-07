package com.pot.member.service.application.command;

import lombok.Builder;

@Builder
public record UpdateMemberProfileCommand(
        Long memberId,
        String nickname,
        String firstName,
        String lastName,
        Integer gender,
        String birthDate,
        String bio,
        String countryCode,
        String region,
        String city,
        String timezone,
        String locale) {
}
