package com.pot.auth.domain.authentication.valueobject;

public record TokenPair(
        JwtToken accessToken,
        RefreshToken refreshToken
) {

        public TokenPair {
        if (accessToken == null) {
            throw new IllegalArgumentException("AccessToken不能为空");
        }
        if (refreshToken == null) {
            throw new IllegalArgumentException("RefreshToken不能为空");
        }
        if (!accessToken.userId().equals(refreshToken.userId())) {
            throw new IllegalArgumentException("AccessToken和RefreshToken的用户ID不一致");
        }
        if (!accessToken.userDomain().equals(refreshToken.userDomain())) {
            throw new IllegalArgumentException("AccessToken和RefreshToken的用户域不一致");
        }
    }

        public String getAccessTokenString() {
        return accessToken.rawToken();
    }

        public String getRefreshTokenString() {
        return refreshToken.rawToken();
    }

        public boolean isAccessTokenExpired() {
        return accessToken.isExpired();
    }

        public boolean isRefreshTokenExpired() {
        return refreshToken.isExpired();
    }

        public boolean isValid() {
        return !isAccessTokenExpired() && !isRefreshTokenExpired();
    }
}

