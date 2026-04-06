package com.pot.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for JWT token issuance.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "auth.token.jwt")
public class JwtProperties {

    private String privateKeyLocation = "classpath:keys/jwt_private_key.pem";

    private String publicKeyLocation = "classpath:keys/jwt_public_key.pem";

    /**
     * Access-token time to live in seconds.
     */
    private long accessTokenTtl = 3600; // 1小时

    /**
     * Refresh-token time to live in seconds.
     */
    private long refreshTokenTtl = 2592000; // 30天

    /**
     * Sliding refresh window in seconds.
     */
    private long refreshTokenSlidingWindow = 604800; // 7天
}
