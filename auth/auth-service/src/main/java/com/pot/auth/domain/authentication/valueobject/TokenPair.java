package com.pot.auth.domain.authentication.valueobject;

/**
 * Token对值对象
 *
 * <p>封装AccessToken和RefreshToken的配对关系
 *
 * @author pot
 * @since 2025-11-10
 */
public record TokenPair(
        JwtToken accessToken,
        RefreshToken refreshToken
) {

    /**
     * 验证参数
     */
    public TokenPair {
        if (accessToken == null) {
            throw new IllegalArgumentException("AccessToken不能为空");
        }
        if (refreshToken == null) {
            throw new IllegalArgumentException("RefreshToken不能为空");
        }
        // 验证两个Token的用户ID和域必须一致
        if (!accessToken.userId().equals(refreshToken.userId())) {
            throw new IllegalArgumentException("AccessToken和RefreshToken的用户ID不一致");
        }
        if (!accessToken.userDomain().equals(refreshToken.userDomain())) {
            throw new IllegalArgumentException("AccessToken和RefreshToken的用户域不一致");
        }
    }

    /**
     * 获取AccessToken的原始字符串
     */
    public String getAccessTokenString() {
        return accessToken.rawToken();
    }

    /**
     * 获取RefreshToken的原始字符串
     */
    public String getRefreshTokenString() {
        return refreshToken.rawToken();
    }

    /**
     * 检查AccessToken是否已过期
     */
    public boolean isAccessTokenExpired() {
        return accessToken.isExpired();
    }

    /**
     * 检查RefreshToken是否已过期
     */
    public boolean isRefreshTokenExpired() {
        return refreshToken.isExpired();
    }

    /**
     * 检查两个Token是否都有效
     */
    public boolean isValid() {
        return !isAccessTokenExpired() && !isRefreshTokenExpired();
    }
}

