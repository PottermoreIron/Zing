package com.pot.auth.application.dto;

/**
 * Application response model for register flows.
 */
public record RegisterResponse(
                Long userId,
                String userDomain,
                String nickname,
                String email,
                String phoneNumber,
                String accessToken,
                String refreshToken,
                Long accessTokenExpiresAt,
                Long refreshTokenExpiresAt,
                String message) {

        /**
         * Creates a successful register response.
         */
        public static RegisterResponse success(
                        Long userId,
                        String userDomain,
                        String nickname,
                        String email,
                        String phoneNumber,
                        String accessToken,
                        String refreshToken,
                        Long accessTokenExpiresAt,
                        Long refreshTokenExpiresAt) {
                return new RegisterResponse(
                                userId,
                                userDomain,
                                nickname,
                                email,
                                phoneNumber,
                                accessToken,
                                refreshToken,
                                accessTokenExpiresAt,
                                refreshTokenExpiresAt,
                                "注册成功");
        }
}
