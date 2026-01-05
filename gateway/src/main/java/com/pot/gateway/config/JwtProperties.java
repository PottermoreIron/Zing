package com.pot.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.jwt")
public class JwtProperties {

    /**
     * RSA公钥文件路径
     */
    private String publicKeyLocation = "classpath:keys/jwt_public_key.pem";

    /**
     * RSA公钥内容（Base64编码，可直接配置）
     */
    private String publicKey;
}
