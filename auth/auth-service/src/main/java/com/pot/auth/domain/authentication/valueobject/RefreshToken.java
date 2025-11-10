package com.pot.auth.domain.authentication.valueobject;

import com.pot.auth.domain.shared.valueobject.DeviceId;
import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

/**
 * 刷新Token值对象
 *
 * <p>封装RefreshToken的业务含义和滑动窗口续期规则
 *
 * @author yecao
 * @since 2025-11-10
 */
public record RefreshToken(
        TokenId tokenId,              // JTI - Token唯一标识
        UserId userId,                // 用户ID
        UserDomain userDomain,        // 用户域
        DeviceId deviceId,            // 设备ID
        Long issuedAt,                // 签发时间（Unix时间戳）
        Long expiresAt,               // 过期时间（Unix时间戳）
        String rawToken               // 原始Token字符串
) {

    /**
     * 验证参数
     */
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

    /**
     * 检查Token是否已过期
     */
    public boolean isExpired() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        return currentTimestamp >= expiresAt;
    }

    /**
     * 检查是否在滑动窗口内（最后7天）
     *
     * @param slidingWindowSeconds 滑动窗口时长（秒）
     * @return 是否在滑动窗口内
     */
    public boolean isWithinSlidingWindow(long slidingWindowSeconds) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long remainingSeconds = expiresAt - currentTimestamp;
        return remainingSeconds <= slidingWindowSeconds && remainingSeconds > 0;
    }

    /**
     * 获取剩余有效时间（秒）
     */
    public long getRemainingSeconds() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long remaining = expiresAt - currentTimestamp;
        return Math.max(0, remaining);
    }

    /**
     * 计算新的过期时间（用于滑动窗口续期）
     *
     * @param refreshTokenTtl RefreshToken总TTL（秒）
     * @return 新的过期时间戳
     */
    public long calculateNewExpiresAt(long refreshTokenTtl) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        return currentTimestamp + refreshTokenTtl;
    }
}

