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
 * JWT配置
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
     * 加载RSA公钥
     */
    @Bean
    public PublicKey jwtPublicKey() throws Exception {
        try {
            // 优先使用配置的公钥字符串
            if (jwtProperties.getPublicKey() != null && !jwtProperties.getPublicKey().isEmpty()) {
                log.info("[JWT配置] 从配置属性加载公钥");
                return loadPublicKeyFromString(jwtProperties.getPublicKey());
            }

            // 否则从文件加载
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

    /**
     * 从字符串加载公钥
     */
    private PublicKey loadPublicKeyFromString(String keyContent) throws Exception {
        // 移除PEM头尾和换行符
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
