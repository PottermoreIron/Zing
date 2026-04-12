package com.pot.auth.domain.authentication.valueobject;

public record TokenPair(
        JwtToken accessToken,
        RefreshToken refreshToken
) {

        public TokenPair {
        if (accessToken == null) {
            throw new IllegalArgumentException("AccessToken must not be null");
        }
        if (refreshToken == null) {
            throw new IllegalArgumentException("RefreshToken must not be null");
        }
        if (!accessToken.userId().equals(refreshToken.userId())) {
            throw new IllegalArgumentException("AccessToken and RefreshToken have mismatched user IDs");
        }
        if (!accessToken.userDomain().equals(refreshToken.userDomain())) {
            throw new IllegalArgumentException("AccessToken and RefreshToken have mismatched user domains");
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

