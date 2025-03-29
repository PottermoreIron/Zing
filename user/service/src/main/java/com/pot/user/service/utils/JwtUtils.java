package com.pot.user.service.utils;

import com.pot.user.service.controller.response.Tokens;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
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
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (MalformedJwtException e) {
            throw new JwtException("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            throw new JwtException("Expired JWT token: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            throw new JwtException("Unsupported JWT token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new JwtException("JWT token compact of handler are invalid: " + e.getMessage());
        } catch (Exception e) {
            throw new JwtException("JWT token is invalid: " + e.getMessage());
        }
    }

    public static Long getUid(String token) {
        try {
            Claims claims = parseToken(token); // 可能抛出BusinessException
            return Optional.ofNullable(claims.get("uid"))
                    .map(Object::toString)
                    .map(Long::parseLong)
                    .orElseThrow(() ->
                            new JwtException("No Invalid uid in token")
                    );
        } catch (NumberFormatException e) {
            throw new JwtException("Invalid uid in token");
        }
    }

    public static void main(String[] args) {
        String token = createAccessToken("123");
        System.out.println(getUid(token));
    }
}