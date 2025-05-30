package com.pot.user.service.utils;

import com.pot.user.service.controller.response.Tokens;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Jwt工具类
 */
@Component
public class JwtUtils {

    private static final String SECRET = "POTISTHEBESTPOTISTHEBESTPOTISTHEBESTPOTISTHEBEST";
    private static final Long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 60;
    private static final Long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7;

    private static final String JWT_ISS = "POT";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    private static final String SUBJECT = "zing";

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "JWT";


    private static String createToken(Map<String, Object> claims, Long expiration) {
        // todo collectionUtils
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("claims must not be null or empty");
        }
        String id = UUID.randomUUID().toString();
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);
        JwtBuilder builder = Jwts.builder()
                .id(id)
                .subject(SUBJECT)
                .signWith(KEY)
                .issuer(JWT_ISS)
                .issuedAt(now)
                .expiration(expireDate);
        claims.forEach(builder::claim);
        return builder.compact();
    }

    private static String createToken(String claim, Object value, Long expiration) {
        Map<String, Object> claims = Map.of(claim, value);
        return createToken(claims, expiration);
    }

    public static String createAccessToken(Object claim) {
        return createToken("uid", claim, ACCESS_TOKEN_EXPIRATION);
    }

    public static String createRefreshToken(Object claim) {
        return createToken("uid", claim, REFRESH_TOKEN_EXPIRATION);
    }

    public static Tokens createAccessTokenAndRefreshToken(Object claim) {
        // 创建访问令牌和刷新令牌
        String accessToken = createAccessToken(claim);
        String refreshToken = createRefreshToken(claim);
        // 返回令牌对象
        return Tokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public static Claims parseToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new JwtException("Token不能为空");
        }

        try {
            return Jwts.parser()
                    .verifyWith(KEY)
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

    public static Long getUid(String token) {
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

    public static Long getUid(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            throw new JwtException("Authorization header is missing or invalid");
        }
        String token = header.substring(TOKEN_PREFIX.length());
        return getUid(token);
    }

    public static void main(String[] args) {
        String token = createAccessToken("123");
        System.out.println(getUid(token));
    }
}