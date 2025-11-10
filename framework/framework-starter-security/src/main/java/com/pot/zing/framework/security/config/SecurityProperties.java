package com.pot.zing.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Security配置属性
 *
 * @author Pot
 * @since 2025-01-24
 */
@Data
@ConfigurationProperties(prefix = "zing.security")
public class SecurityProperties {

    /**
     * 是否启用Security
     */
    private boolean enabled = true;

    /**
     * JWT配置
     */
    private JwtProperties jwt = new JwtProperties();

    /**
     * 白名单配置
     */
    private List<String> whitelist = new ArrayList<>();

    /**
     * 是否启用CSRF保护
     */
    private boolean csrfEnabled = false;

    /**
     * 会话管理策略
     */
    private SessionStrategy sessionStrategy = SessionStrategy.STATELESS;

    /**
     * 权限缓存配置
     */
    private CacheProperties cache = new CacheProperties();

    /**
     * 会话策略枚举
     */
    public enum SessionStrategy {
        /**
         * 无状态（JWT）
         */
        STATELESS,
        /**
         * 有状态（Session）
         */
        STATEFUL
    }

    @Data
    public static class JwtProperties {
        /**
         * JWT密钥
         */
        private String secretKey = "your-256-bit-secret-key-change-this-in-production-environment-please";

        /**
         * AccessToken有效期（毫秒）
         */
        private long accessTokenValidity = 3600000L; // 1小时

        /**
         * RefreshToken有效期（毫秒）
         */
        private long refreshTokenValidity = 2592000000L; // 30天

        /**
         * Token请求头名称
         */
        private String header = "Authorization";

        /**
         * Token前缀
         */
        private String prefix = "Bearer ";

        /**
         * Token签发者
         */
        private String issuer = "zing";
    }

    @Data
    public static class CacheProperties {
        /**
         * 是否启用权限缓存
         */
        private boolean enabled = true;

        /**
         * 缓存过期时间（秒）
         */
        private long ttl = 1800L; // 30分钟
    }
}