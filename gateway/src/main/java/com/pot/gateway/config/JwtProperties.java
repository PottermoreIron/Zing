package com.pot.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized properties for JWT public key loading.
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway.jwt")
public class JwtProperties {

    /** Resource location of the RSA public key file. */
    private String publicKeyLocation = "classpath:keys/jwt_public_key.pem";

    /**
     * Inline RSA public key content, typically provided as Base64-encoded PEM text.
     */
    private String publicKey;
}
