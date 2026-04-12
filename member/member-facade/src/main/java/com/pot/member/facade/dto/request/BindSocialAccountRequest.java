package com.pot.member.facade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record BindSocialAccountRequest(
        @NotNull(message = "Member ID must not be null") Long memberId,
        @NotBlank(message = "Provider must not be blank") String provider,
        @NotBlank(message = "Provider member ID must not be blank") String providerMemberId,
        String providerUsername,
        String providerEmail,
        @NotBlank(message = "Access token must not be blank") String accessToken,
        String refreshToken,
        Long tokenExpiresAt,
        String scope,
        String extendJson) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
