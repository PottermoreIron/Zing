package com.pot.auth.application.dto;

/**
 * Application response model for login flows.
 */
public record LoginResponse(
        Long userId,
        String userDomain,
        String nickname,
        String email,
        String phoneNumber,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresAt,
        Long refreshTokenExpiresAt) {
}
