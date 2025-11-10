package com.pot.auth.domain.authentication.valueobject;

import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

import java.util.Set;

/**
 * JWT Token值对象
 *
 * <p>封装JWT Token的业务含义和验证规则
 *
 * @author yecao
 * @since 2025-11-10
 */
public record JwtToken(
        TokenId tokenId,              // JTI - Token唯一标识
        UserId userId,                // 用户ID
        UserDomain userDomain,        // 用户域
        String username,              // 用户名
        Set<String> authorities,      // 权限列表
        Long issuedAt,                // 签发时间（Unix时间戳）
        Long expiresAt,               // 过期时间（Unix时间戳）
        String rawToken               // 原始Token字符串
) {

    /**
     * 验证参数
     */
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
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
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
    }

    /**
     * 检查Token是否已过期
     */
    public boolean isExpired() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        return currentTimestamp >= expiresAt;
    }

    /**
     * 检查Token是否即将过期（剩余时间少于指定秒数）
     */
    public boolean isExpiringSoon(long thresholdSeconds) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long remainingSeconds = expiresAt - currentTimestamp;
        return remainingSeconds <= thresholdSeconds;
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
     * 检查是否拥有指定权限
     */
    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    /**
     * 检查是否拥有任一权限
     */
    public boolean hasAnyAuthority(Set<String> requiredAuthorities) {
        return requiredAuthorities.stream().anyMatch(authorities::contains);
    }

    /**
     * 检查是否拥有所有权限
     */
    public boolean hasAllAuthorities(Set<String> requiredAuthorities) {
        return authorities.containsAll(requiredAuthorities);
    }
}

