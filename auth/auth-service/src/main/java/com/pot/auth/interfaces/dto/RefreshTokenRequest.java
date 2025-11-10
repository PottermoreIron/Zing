package com.pot.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新Token请求
 *
 * @author yecao
 * @since 2025-11-10
 */
public record RefreshTokenRequest(
        @NotBlank(message = "RefreshToken不能为空")
        String refreshToken
) {
}

