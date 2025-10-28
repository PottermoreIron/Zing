package com.pot.zing.framework.security.jwt;

import com.pot.zing.framework.security.config.SecurityProperties;
import com.pot.zing.framework.security.core.userdetails.SecurityUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * JWT Token提供者
 * <p>
 * 负责JWT Token的生成、解析和验证
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Getter
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /**
     * -- GETTER --
     * 获取SecurityProperties配置
     */
    private final SecurityProperties securityProperties;

    /**
     * 生成AccessToken
     */
    public String generateAccessToken(SecurityUser user) {
        return generateToken(user, securityProperties.getJwt().getAccessTokenValidity(), TokenType.ACCESS);
    }

    /**
     * 生成RefreshToken
     */
    public String generateRefreshToken(SecurityUser user) {
        return generateToken(user, securityProperties.getJwt().getRefreshTokenValidity(), TokenType.REFRESH);
    }

    /**
     * 生成Token
     */
    private String generateToken(SecurityUser user, long validity, TokenType tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("sessionId", user.getSessionId());
        claims.put("username", user.getUsername());
        claims.put("nickname", user.getNickname());
        claims.put("email", user.getEmail());
        claims.put("phone", user.getPhone());
        claims.put("roles", user.getRoles());
        claims.put("permissions", user.getPermissions());
        claims.put("tokenType", tokenType.name());

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getUserId()))
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(securityProperties.getJwt().getIssuer())
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 从Token中解析用户信息
     */
    public SecurityUser getUserFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }

        return SecurityUser.builder()
                .userId(claims.get("userId", Long.class))
                .sessionId(claims.get("sessionId", String.class))
                .username(claims.get("username", String.class))
                .nickname(claims.get("nickname", String.class))
                .email(claims.get("email", String.class))
                .phone(claims.get("phone", String.class))
                .roles(claims.get("roles", Set.class))
                .permissions(claims.get("permissions", Set.class))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userId", Long.class) : null;
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证Token类型
     */
    public boolean validateTokenType(String token, TokenType expectedType) {
        try {
            Claims claims = parseToken(token);
            String tokenType = claims.get("tokenType", String.class);
            return expectedType.name().equals(tokenType);
        } catch (Exception e) {
            log.debug("Token类型验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查Token是否即将过期（小于5分钟）
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long timeLeft = expiration.getTime() - System.currentTimeMillis();
            return timeLeft < 300000; // 5分钟
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 解析Token
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token已过期: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("不支持的Token: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("Token格式错误: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.error("Token签名验证失败: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Token为空: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = securityProperties.getJwt().getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Token类型枚举
     */
    public enum TokenType {
        /**
         * 访问Token
         */
        ACCESS,
        /**
         * 刷新Token
         */
        REFRESH
    }
}


