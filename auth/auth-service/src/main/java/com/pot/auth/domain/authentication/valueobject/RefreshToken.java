package com.pot.auth.domain.authentication.valueobject;

import com.pot.auth.domain.shared.valueobject.DeviceId;
import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

public record RefreshToken(
        TokenId tokenId,              // JTI - Token唯一标识
        UserId userId,                // 用户ID
        UserDomain userDomain,        // 用户域
        DeviceId deviceId,            // 设备ID
        Long issuedAt,                // 签发时间（Unix时间戳）
        Long expiresAt,               // 过期时间（Unix时间戳）
        String rawToken               // 原始Token字符串
) {

        public RefreshToken {
        if (tokenId == null) {
            throw new IllegalArgumentException("TokenId不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId不能为空");
        }
        if (userDomain == null) {
            throw new IllegalArgumentException("UserDomain不能为空");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("DeviceId不能为空");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("时间戳不能为空");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token字符串不能为空");
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

