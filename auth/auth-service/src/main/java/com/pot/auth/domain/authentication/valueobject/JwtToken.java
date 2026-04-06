package com.pot.auth.domain.authentication.valueobject;

import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

import java.util.Map;
import java.util.Set;

public record JwtToken(
        TokenId tokenId, // JTI - Token唯一标识
        UserId userId, // 用户ID
        UserDomain userDomain, // 用户域
        String nickname, // 显示名
        Set<String> authorities, // 权限列表
        Long issuedAt, // 签发时间（Unix时间戳）
        Long expiresAt, // 过期时间（Unix时间戳）
        String rawToken, // 原始Token字符串
        Map<String, Object> claimsMap // 【新增】完整的Claims Map，用于获取自定义字段
) {

        public JwtToken {
        if (tokenId == null) {
            throw new IllegalArgumentException("TokenId不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId不能为空");
        }
        if (userDomain == null) {
            throw new IllegalArgumentException("UserDomain不能为空");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (authorities == null) {
            authorities = Set.of();
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("时间戳不能为空");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token字符串不能为空");
        }
        if (claimsMap == null) {
            claimsMap = Map.of();
        }
    }

        @SuppressWarnings("unchecked")
    public <T> T getClaim(String key, Class<T> clazz) {
        Object value = claimsMap.get(key);
        if (value == null) {
            return null;
        }

        try {
            if (clazz == Long.class && value instanceof Number) {
                return (T) Long.valueOf(((Number) value).longValue());
            }
            if (clazz == Integer.class && value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            }

            return clazz.cast(value);
        } catch (ClassCastException e) {
            return null;
        }
    }

        public boolean isExpired() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        return currentTimestamp >= expiresAt;
    }

        public boolean isExpiringSoon(long thresholdSeconds) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long remainingSeconds = expiresAt - currentTimestamp;
        return remainingSeconds <= thresholdSeconds;
    }

        public long getRemainingSeconds() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long remaining = expiresAt - currentTimestamp;
        return Math.max(0, remaining);
    }

        public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

        public boolean hasAnyAuthority(Set<String> requiredAuthorities) {
        return requiredAuthorities.stream().anyMatch(authorities::contains);
    }

        public boolean hasAllAuthorities(Set<String> requiredAuthorities) {
        return authorities.containsAll(requiredAuthorities);
    }
}
