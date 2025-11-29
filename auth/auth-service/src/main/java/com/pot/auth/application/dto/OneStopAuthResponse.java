package com.pot.auth.application.dto;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.Builder;

/**
 * 一键认证响应DTO
 *
 * <p>
 * 将领域层的 AuthenticationResult 转换为应用层响应
 *
 * @author pot
 * @since 2025-11-29
 */
@Builder
public record OneStopAuthResponse(
        UserId userId,
        UserDomain userDomain,
        String username,
        String email,
        String phone,
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresAt,
        Long refreshTokenExpiresAt) {

    /**
     * 从领域对象转换
     */
    public static OneStopAuthResponse from(AuthenticationResult result) {
        return OneStopAuthResponse.builder()
                .userId(result.userId())
                .userDomain(result.userDomain())
                .username(result.username())
                .email(result.email())
                .phone(result.phone())
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .accessTokenExpiresAt(result.accessTokenExpiresAt())
                .refreshTokenExpiresAt(result.refreshTokenExpiresAt())
                .build();
    }
}
