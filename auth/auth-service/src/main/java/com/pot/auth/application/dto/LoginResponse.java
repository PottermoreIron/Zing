package com.pot.auth.application.dto;

/**
 * 登录响应DTO
 *
 * @author pot
 * @since 2025-11-10
 */
public record LoginResponse(
        Long userId,
        String userDomain,
        String username,
        String email,
        String phoneNumber,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresAt,
        Long refreshTokenExpiresAt
) {
}

