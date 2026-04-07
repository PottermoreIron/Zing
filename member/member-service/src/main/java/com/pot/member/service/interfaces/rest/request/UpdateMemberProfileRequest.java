package com.pot.member.service.interfaces.rest.request;

/**
 * Transport request for updating a member profile.
 */
public record UpdateMemberProfileRequest(
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