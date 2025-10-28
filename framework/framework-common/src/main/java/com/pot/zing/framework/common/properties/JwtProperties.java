package com.pot.zing.framework.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: Pot
 * @created: 2025/10/19 21:04
 * @description: jwt配置类
 */
@Data
@ConfigurationProperties(prefix = "pot.jwt")
public class JwtProperties {
    /**
     * 是否启用JWT
     */
    private boolean enable = false;

    /**
     * JWT 密钥
     */
    private String secret;

    /**
     * 访问令牌过期时间(毫秒)
     */
    private Long accessTokenExpiration = 3600000L;

    /**
     * 刷新令牌过期时间(毫秒)
     */
    private Long refreshTokenExpiration = 604800000L;

    /**
     * 令牌签发者
     */
    private String issuer = "pot";

    /**
     * 令牌主题
     */
    private String subject = "auth";

    /**
     * 令牌头部名称
     */
    private String tokenHeader = "Authorization";

    /**
     * 令牌前缀
     */
    private String tokenPrefix = "Bearer ";
}
