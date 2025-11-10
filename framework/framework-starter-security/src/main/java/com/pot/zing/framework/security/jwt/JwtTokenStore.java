package com.pot.zing.framework.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * JWT Token存储服务
 * <p>
 * 使用Redis存储Token，支持黑名单和在线用户管理
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenStore {

    private static final String TOKEN_BLACKLIST_PREFIX = "security:token:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "security:token:refresh:";
    private static final String ONLINE_USER_PREFIX = "security:online:user:";
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 将Token加入黑名单
     */
    public void addToBlacklist(String token, long expirationTime) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", expirationTime, TimeUnit.MILLISECONDS);
        log.debug("Token已加入黑名单: {}", token.substring(0, Math.min(20, token.length())));
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 存储RefreshToken
     */
    public void storeRefreshToken(Long userId, String refreshToken, long ttl) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, ttl, TimeUnit.MILLISECONDS);
        log.debug("RefreshToken已存储: userId={}", userId);
    }

    /**
     * 获取RefreshToken
     */
    public String getRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }

    /**
     * 删除RefreshToken
     */
    public void removeRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("RefreshToken已删除: userId={}", userId);
    }

    /**
     * 记录在线用户
     */
    public void recordOnlineUser(Long userId, String token, long ttl) {
        String key = ONLINE_USER_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, ttl, TimeUnit.MILLISECONDS);
        log.debug("在线用户已记录: userId={}", userId);
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        String key = ONLINE_USER_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 移除在线用户
     */
    public void removeOnlineUser(Long userId) {
        String key = ONLINE_USER_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("在线用户已移除: userId={}", userId);
    }

    /**
     * 强制用户下线（将当前Token加入黑名单）
     */
    public void forceLogout(Long userId, String currentToken, long tokenExpiration) {
        addToBlacklist(currentToken, tokenExpiration);
        removeOnlineUser(userId);
        removeRefreshToken(userId);
        log.info("用户已被强制下线: userId={}", userId);
    }
}