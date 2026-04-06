package com.pot.zing.framework.common.util;

import com.pot.zing.framework.common.properties.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT creation and parsing helpers.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;
    private volatile SecretKey secretKey;

    /**
     * Lazily resolves the signing key.
     */
    private SecretKey getSecretKey() {
        if (secretKey == null) {
            synchronized (this) {
                if (secretKey == null) {
                    secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
                }
            }
        }
        return secretKey;
    }

    /**
     * Creates a JWT with the supplied claims and expiration.
     */
    public String createToken(Map<String, Object> claims, Long expiration) {
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("claims must not be null or empty");
        }

        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);

        JwtBuilder builder = Jwts.builder()
                .id(jti)
                .subject(jwtProperties.getSubject())
                .signWith(getSecretKey())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expireDate);

        claims.forEach(builder::claim);
        return builder.compact();
    }

    /**
     * Creates an access token.
     */
    public String createAccessToken(Object userId) {
        return createToken(Map.of("uid", userId), jwtProperties.getAccessTokenExpiration());
    }

    /**
     * Creates a refresh token.
     */
    public String createRefreshToken(Object userId) {
        return createToken(Map.of("uid", userId), jwtProperties.getRefreshTokenExpiration());
    }

    /**
     * Parses and validates a JWT.
     */
    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token不能为空");
        }

        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (MalformedJwtException e) {
            log.error("JWT令牌格式不正确: {}", e.getMessage());
            throw new JwtException("JWT令牌格式不正确", e);
        } catch (ExpiredJwtException e) {
            log.error("JWT令牌已过期: {}", e.getMessage());
            throw new JwtException("JWT令牌已过期", e);
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT令牌格式: {}", e.getMessage());
            throw new JwtException("不支持的JWT令牌格式", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT令牌参数无效: {}", e.getMessage());
            throw new JwtException("JWT令牌参数无效", e);
        } catch (Exception e) {
            log.error("JWT令牌解析失败: {}", e.getMessage());
            throw new JwtException("JWT令牌解析失败", e);
        }
    }

    /**
     * Resolves a claim from a JWT.
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = parseToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts the user ID from a JWT.
     */
    public Long getUid(String token) {
        return getClaimFromToken(token, claims -> {
            Object uidObj = claims.get("uid");
            if (uidObj == null) {
                throw new JwtException("令牌中不包含uid信息");
            }
            try {
                return Long.parseLong(uidObj.toString());
            } catch (NumberFormatException e) {
                throw new JwtException("令牌中uid格式无效", e);
            }
        });
    }

    /**
     * Extracts the user ID from an HTTP request.
     */
    public Long getUid(HttpServletRequest request) {
        String token = extractToken(request);
        return getUid(token);
    }

    /**
     * Extracts the token from the configured authorization header.
     */
    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getTokenHeader());
        if (header == null || !header.startsWith(jwtProperties.getTokenPrefix())) {
            throw new JwtException("Authorization header is missing or invalid");
        }
        return header.substring(jwtProperties.getTokenPrefix().length());
    }

    /**
     * Returns whether the token is valid.
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.debug("令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns whether the token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaimFromToken(token, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}
