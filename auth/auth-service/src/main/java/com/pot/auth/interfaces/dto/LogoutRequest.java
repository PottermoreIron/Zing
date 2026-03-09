package com.pot.auth.interfaces.dto;

/**
 * 登出请求
 *
 * <p>
 * refreshToken 可选；提供时会同步吊销 RefreshToken（防止利用旧 RefreshToken 重新登录）。
 *
 * @author pot
 * @since 2025-12-14
 */
public record LogoutRequest(
        /**
         * Refresh Token（可选）
         * <p>
         * 提供时会从 Redis 中删除对应的 RefreshToken 缓存，彻底阻止续期。
         */
        String refreshToken) {
}
