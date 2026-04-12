package com.pot.auth.domain.authentication.valueobject;

import com.pot.auth.domain.shared.valueobject.DeviceId;
import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

public record RefreshToken(
        TokenId tokenId,              // JTI - unique token identifier
        UserId userId,                // user ID
        UserDomain userDomain,        // user domain
        DeviceId deviceId,            // device ID
        Long issuedAt,                // issued at (Unix timestamp)
        Long expiresAt,               // expires at (Unix timestamp)
        String rawToken               // raw token string
) {

        public RefreshToken {
        if (tokenId == null) {
            throw new IllegalArgumentException("TokenId must not be blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId must not be null");
        }
        if (userDomain == null) {
            throw new IllegalArgumentException("UserDomain must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("DeviceId must not be null");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Timestamps must not be null");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token string must not be blank");
        }
    }

        public boolean isExpired() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        return currentTimestamp >= expiresAt;
    }

        public boolean isWithinSlidingWindow(long slidingWindowSeconds) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long remainingSeconds = expiresAt - currentTimestamp;
        return remainingSeconds <= slidingWindowSeconds && remainingSeconds > 0;
    }

        public long getRemainingSeconds() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long remaining = expiresAt - currentTimestamp;
        return Math.max(0, remaining);
    }

        public long calculateNewExpiresAt(long refreshTokenTtl) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        return currentTimestamp + refreshTokenTtl;
    }
}

