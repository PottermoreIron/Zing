package com.pot.auth.application.dto;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.Builder;

/**
 * Application response model for one-stop authentication flows.
 */
@Builder
public record OneStopAuthResponse(
        UserId userId,
        UserDomain userDomain,
        String nickname,
        String email,
        String phone,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresAt,
        Long refreshTokenExpiresAt) {

    /**
     * Maps a domain authentication result to the response model.
     */
    public static OneStopAuthResponse from(AuthenticationResult result) {
        return OneStopAuthResponse.builder()
                .userId(result.userId())
                .userDomain(result.userDomain())
                .nickname(result.nickname())
                .email(result.email())
                .phone(result.phone())
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .accessTokenExpiresAt(result.accessTokenExpiresAt())
                .refreshTokenExpiresAt(result.refreshTokenExpiresAt())
                .build();
    }
}
