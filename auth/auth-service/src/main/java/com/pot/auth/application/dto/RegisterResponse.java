package com.pot.auth.application.dto;

/**
 * 注册响应DTO
 *
 * <p>注册成功后自动登录，返回Token信息
 *
 * @author yecao
 * @since 2025-11-10
 */
public record RegisterResponse(
        Long userId,
        String userDomain,
        String username,
        String email,
        String phoneNumber,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresAt,
        Long refreshTokenExpiresAt,
        String message
) {

    /**
     * 成功响应
     */
    public static RegisterResponse success(
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
        return new RegisterResponse(
                userId,
                userDomain,
                username,
                email,
                phoneNumber,
                accessToken,
                refreshToken,
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                "注册成功"
        );
    }
}

