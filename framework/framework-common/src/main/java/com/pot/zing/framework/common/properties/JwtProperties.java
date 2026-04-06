package com.pot.zing.framework.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for shared JWT utilities.
 */
@Data
@ConfigurationProperties(prefix = "pot.jwt")
public class JwtProperties {
    private boolean enable = false;

    private String secret;

    /**
     * Access-token expiration in milliseconds.
     */
    private Long accessTokenExpiration = 3600000L;

    /**
     * Refresh-token expiration in milliseconds.
     */
    private Long refreshTokenExpiration = 604800000L;

    private String issuer = "pot";

    private String subject = "auth";

    private String tokenHeader = "Authorization";

    private String tokenPrefix = "Bearer ";
}
