package com.pot.auth.infrastructure.adapter.token;

import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.RefreshToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import com.pot.auth.domain.port.TokenManagementPort;
import com.pot.auth.domain.shared.valueobject.DeviceId;
import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Spring Security JWT适配器
 *
 * <p>实现TokenManagementPort接口，使用JWT进行Token管理
 *
 * @author pot
 * @since 2025-11-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringSecurityJwtAdapter implements TokenManagementPort {

    private final JwtProperties jwtProperties;
    private final ResourceLoader resourceLoader;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * 初始化RSA密钥对
     */
    @PostConstruct
    public void init() {
        try {
            // 加载私钥
            Resource privateKeyResource = resourceLoader.getResource(jwtProperties.getPrivateKeyLocation());
            byte[] privateKeyBytes = Files.readAllBytes(privateKeyResource.getFile().toPath());
            String privateKeyPEM = new String(privateKeyBytes)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] privateKeyDecoded = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyDecoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = keyFactory.generatePrivate(privateKeySpec);

            // 加载公钥
            Resource publicKeyResource = resourceLoader.getResource(jwtProperties.getPublicKeyLocation());
            byte[] publicKeyBytes = Files.readAllBytes(publicKeyResource.getFile().toPath());
            String publicKeyPEM = new String(publicKeyBytes)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] publicKeyDecoded = Base64.getDecoder().decode(publicKeyPEM);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyDecoded);
            this.publicKey = keyFactory.generatePublic(publicKeySpec);

            log.info("[JWT] RSA密钥对加载成功");
        } catch (Exception e) {
            log.error("[JWT] RSA密钥对加载失败", e);
            throw new RuntimeException("RSA密钥对加载失败", e);
        }
    }

    @Override
    public TokenPair generateTokenPair(
            UserId userId,
            UserDomain userDomain,
            String username,
            Set<String> authorities
    ) {
        long currentTime = System.currentTimeMillis() / 1000;

        // 生成AccessToken
        TokenId accessTokenId = TokenId.generate();
        long accessTokenExpiresAt = currentTime + jwtProperties.getAccessTokenTtl();

        String accessTokenString = Jwts.builder()
                .id(accessTokenId.value())
                .subject(userId.value().toString())
                .claim("userId", userId.value())
                .claim("userDomain", userDomain.name())
                .claim("username", username)
                .claim("authorities", authorities)
                .issuedAt(new Date(currentTime * 1000))
                .expiration(new Date(accessTokenExpiresAt * 1000))
                .signWith(privateKey)
                .compact();

        JwtToken accessToken = new JwtToken(
                accessTokenId,
                userId,
                userDomain,
                username,
                authorities,
                currentTime,
                accessTokenExpiresAt,
                accessTokenString
        );

        // 生成RefreshToken
        TokenId refreshTokenId = TokenId.generate();
        long refreshTokenExpiresAt = currentTime + jwtProperties.getRefreshTokenTtl();
        DeviceId deviceId = new DeviceId(1L); // TODO: 实际设备ID

        String refreshTokenString = Jwts.builder()
                .id(refreshTokenId.value())
                .subject(userId.value().toString())
                .claim("userId", userId.value())
                .claim("userDomain", userDomain.name())
                .claim("deviceId", deviceId.value())
                .claim("type", "refresh")
                .issuedAt(new Date(currentTime * 1000))
                .expiration(new Date(refreshTokenExpiresAt * 1000))
                .signWith(privateKey)
                .compact();

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenId,
                userId,
                userDomain,
                deviceId,
                currentTime,
                refreshTokenExpiresAt,
                refreshTokenString
        );

        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public JwtToken parseAccessToken(String tokenString) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(tokenString)
                    .getPayload();

            TokenId tokenId = new TokenId(claims.getId());
            UserId userId = UserId.of(((Number) claims.get("userId")).longValue());
            UserDomain userDomain = UserDomain.valueOf((String) claims.get("userDomain"));
            String username = (String) claims.get("username");

            @SuppressWarnings("unchecked")
            List<String> authList = (List<String>) claims.get("authorities");
            Set<String> authorities = authList != null ? new HashSet<>(authList) : Set.of();

            long issuedAt = claims.getIssuedAt().getTime() / 1000;
            long expiresAt = claims.getExpiration().getTime() / 1000;

            return new JwtToken(
                    tokenId,
                    userId,
                    userDomain,
                    username,
                    authorities,
                    issuedAt,
                    expiresAt,
                    tokenString
            );
        } catch (Exception e) {
            log.error("[JWT] AccessToken解析失败", e);
            throw new RuntimeException("Token解析失败", e);
        }
    }

    @Override
    public RefreshToken parseRefreshToken(String tokenString) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(tokenString)
                    .getPayload();

            TokenId tokenId = TokenId.of(claims.getId());
            UserId userId = UserId.of(((Number) claims.get("userId")).longValue());
            UserDomain userDomain = UserDomain.valueOf((String) claims.get("userDomain"));
            DeviceId deviceId = DeviceId.of(((Number) claims.get("deviceId")).longValue());

            long issuedAt = claims.getIssuedAt().getTime() / 1000;
            long expiresAt = claims.getExpiration().getTime() / 1000;

            return new RefreshToken(
                    tokenId,
                    userId,
                    userDomain,
                    deviceId,
                    issuedAt,
                    expiresAt,
                    tokenString
            );
        } catch (Exception e) {
            log.error("[JWT] RefreshToken解析失败", e);
            throw new RuntimeException("Token解析失败", e);
        }
    }
}

