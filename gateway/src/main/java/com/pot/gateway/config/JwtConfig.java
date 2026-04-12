package com.pot.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads the RSA public key used to validate gateway JWTs.
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtProperties jwtProperties;
    private final ResourceLoader resourceLoader;

    /**
     * Creates the public key bean from inline content or the configured resource.
     */
    @Bean
    public PublicKey jwtPublicKey() {
        try {
            if (hasInlinePublicKey()) {
                log.info("[JwtConfig] Loading public key from configuration property");
                return loadPublicKeyFromString(jwtProperties.getPublicKey());
            }

            String location = jwtProperties.getPublicKeyLocation();
            log.info("[JwtConfig] Loading public key from file — location={}", location);

            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                throw new GatewayConfigurationException("JWT public key file not found: " + location);
            }

            String keyContent = readKeyContent(resource);
            return loadPublicKeyFromString(keyContent);

        } catch (Exception e) {
            log.error("[JwtConfig] Failed to load public key", e);
            throw new GatewayConfigurationException(
                    "Failed to load JWT public key. Check configuration: gateway.jwt.publicKeyLocation or gateway.jwt.publicKey",
                    e);
        }
    }

    private boolean hasInlinePublicKey() {
        String publicKey = jwtProperties.getPublicKey();
        return publicKey != null && !publicKey.isBlank();
    }

    private String readKeyContent(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private PublicKey loadPublicKeyFromString(String keyContent) throws Exception {
        String publicKeyPEM = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        PublicKey publicKey = keyFactory.generatePublic(spec);
        log.info("[JwtConfig] Public key loaded successfully");

        return publicKey;
    }
}
