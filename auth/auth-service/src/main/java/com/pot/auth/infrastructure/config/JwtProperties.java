package com.pot.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for JWT token issuance.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.token.jwt")
public class JwtProperties {

    private String privateKeyLocation = "classpath:keys/jwt_private_key.pem";

    private String publicKeyLocation = "classpath:keys/jwt_public_key.pem";

    /**
     * Access-token time to live in seconds.
     */
    private long accessTokenTtl = 3600; // 1 hour

    /**
     * Refresh-token time to live in seconds.
     */
    private long refreshTokenTtl = 2592000; // 30 days

    /**
     * Sliding refresh window in seconds.
     */
    private long refreshTokenSlidingWindow = 604800; // 7 days
}
