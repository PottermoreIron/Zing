package com.pot.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置属性
 *
 * @author yecao
 * @since 2025-11-10
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "auth.token.jwt")
public class JwtProperties {

    /**
     * RSA私钥文件路径
     */
    private String privateKeyLocation = "classpath:keys/jwt_private_key.pem";

    /**
     * RSA公钥文件路径
     */
    private String publicKeyLocation = "classpath:keys/jwt_public_key.pem";

    /**
     * AccessToken过期时间（秒）
     */
    private long accessTokenTtl = 3600; // 1小时

    /**
     * RefreshToken过期时间（秒）
     */
    private long refreshTokenTtl = 2592000; // 30天

    /**
     * RefreshToken滑动窗口（秒）
     */
    private long refreshTokenSlidingWindow = 604800; // 7天
}

