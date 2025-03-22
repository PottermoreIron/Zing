package com.pot.user.service.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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


    private static String createToken(Map<String, String> claims, Long expiration) {
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

    private static String createToken(String claim, String value, Long expiration) {
        Map<String, String> claims = Map.of(claim, value);
        return createToken(claims, expiration);
    }

    public static String createAccessToken(String claim) {
        return createToken("uid", claim, ACCESS_TOKEN_EXPIRATION);
    }

    public static String createRefreshToken(String claim) {
        return createToken("uid", claim, REFRESH_TOKEN_EXPIRATION);
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static void main(String[] args) {
    }
}