package com.pot.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author: Pot
 * @created: 2025/8/16 22:07
 * @description: Jwt工具类
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    @Value("${jwt.subject}")
    private String subject;
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "JWT";


    public String createToken(Map<String, Object> claims, Long expiration) {
        // todo collectionUtils
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("claims must not be null or empty");
        }
        String id = UUID.randomUUID().toString();
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        JwtBuilder builder = Jwts.builder()
                .id(id)
                .subject(subject)
                .signWith(key)
                .issuer(jwtIssuer)
                .issuedAt(now)
                .expiration(expireDate);
        claims.forEach(builder::claim);
        return builder.compact();
    }

    private String createToken(String claim, Object value, Long expiration) {
        Map<String, Object> claims = Map.of(claim, value);
        return createToken(claims, expiration);
    }

    public String createAccessToken(Object claim) {
        return createToken("uid", claim, accessTokenExpiration);
    }

    public String createRefreshToken(Object claim) {
        return createToken("uid", claim, refreshTokenExpiration);
    }

    public Claims parseToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new JwtException("Token不能为空");
        }
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (MalformedJwtException e) {
            throw new JwtException("JWT令牌格式不正确", e);
        } catch (ExpiredJwtException e) {
            throw new JwtException("JWT令牌已过期", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtException("不支持的JWT令牌格式", e);
        } catch (IllegalArgumentException e) {
            throw new JwtException("JWT令牌参数无效", e);
        } catch (Exception e) {
            throw new JwtException("JWT令牌解析失败：" + e.getMessage(), e);
        }
    }

    public Long getUid(String token) {
        try {
            Claims claims = parseToken(token);
            Object uidObj = claims.get("uid");

            if (uidObj == null) {
                throw new JwtException("令牌中不包含uid信息");
            }

            try {
                return Long.parseLong(uidObj.toString());
            } catch (NumberFormatException e) {
                throw new JwtException("令牌中uid格式无效，无法转换为数字", e);
            }
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("获取用户ID时发生错误", e);
        }
    }

    public Long getUid(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            throw new JwtException("Authorization header is missing or invalid");
        }
        String token = header.substring(TOKEN_PREFIX.length());
        return getUid(token);
    }
}
