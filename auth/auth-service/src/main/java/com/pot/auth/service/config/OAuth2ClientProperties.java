package com.pot.auth.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2客户端配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2ClientProperties {

    /**
     * 各个OAuth2提供商的配置
     */
    private Map<String, OAuth2ClientConfig> clients = new HashMap<>();

    /**
     * OAuth2客户端配置
     */
    @Data
    public static class OAuth2ClientConfig {
        /**
         * 客户端ID
         */
        private String clientId;

        /**
         * 客户端密钥
         */
        private String clientSecret;

        /**
         * 授权端点
         */
        private String authorizationUri;

        /**
         * Token端点
         */
        private String tokenUri;

        /**
         * 用户信息端点
         */
        private String userInfoUri;

        /**
         * 重定向URI
         */
        private String redirectUri;

        /**
         * 请求的权限范围
         */
        private String scope;

        /**
         * 是否启用
         */
        private Boolean enabled = false;
    }
}
