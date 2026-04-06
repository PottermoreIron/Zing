package com.pot.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.file.Files;
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
    public PublicKey jwtPublicKey() throws Exception {
        try {
            if (jwtProperties.getPublicKey() != null && !jwtProperties.getPublicKey().isEmpty()) {
                log.info("[JWT配置] 从配置属性加载公钥");
                return loadPublicKeyFromString(jwtProperties.getPublicKey());
            }

            String location = jwtProperties.getPublicKeyLocation();
            log.info("[JWT配置] 从文件加载公钥: {}", location);

            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                throw new IllegalStateException("JWT公钥文件不存在: " + location);
            }

            String keyContent = Files.readString(resource.getFile().toPath());
            return loadPublicKeyFromString(keyContent);

        } catch (Exception e) {
            log.error("[JWT配置] 加载公钥失败", e);
            throw new IllegalStateException("无法加载JWT公钥，请检查配置: gateway.jwt.publicKeyLocation 或 gateway.jwt.publicKey",
                    e);
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
        log.info("[JWT配置] 公钥加载成功");

        return publicKey;
    }
}
