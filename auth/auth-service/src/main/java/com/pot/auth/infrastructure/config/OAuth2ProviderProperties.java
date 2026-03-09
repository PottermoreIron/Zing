package com.pot.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 提供商配置属性
 *
 * <p>
 * 每个提供商单独配置 clientId、clientSecret 及端点 URL，
 * 便于按需启用/禁用，无需改代码。
 *
 * <p>
 * 配置示例（application.yml）：
 * 
 * <pre>
 * auth:
 *   oauth2:
 *     enabled: true
 *     providers:
 *       google:
 *         client-id: your-client-id
 *         client-secret: your-client-secret
 *         token-url: https://oauth2.googleapis.com/token
 *         user-info-url: https://www.googleapis.com/oauth2/v3/userinfo
 *       github:
 *         client-id: your-client-id
 *         client-secret: your-client-secret
 *         token-url: https://github.com/login/oauth/access_token
 *         user-info-url: https://api.github.com/user
 * </pre>
 *
 * @author pot
 * @since 2025-12-14
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.oauth2")
public class OAuth2ProviderProperties {

    /**
     * 是否启用 OAuth2 功能（开关，默认关闭）
     */
    private boolean enabled = false;

    /**
     * 各提供商配置，key 为提供商 code（google/github/facebook/apple）
     */
    private Map<String, ProviderConfig> providers = defaultProviders();

    private static Map<String, ProviderConfig> defaultProviders() {
        Map<String, ProviderConfig> map = new HashMap<>();

        ProviderConfig google = new ProviderConfig();
        google.setTokenUrl("https://oauth2.googleapis.com/token");
        google.setUserInfoUrl("https://www.googleapis.com/oauth2/v3/userinfo");
        google.setRefreshTokenUrl("https://oauth2.googleapis.com/token");
        map.put("google", google);

        ProviderConfig github = new ProviderConfig();
        github.setTokenUrl("https://github.com/login/oauth/access_token");
        github.setUserInfoUrl("https://api.github.com/user");
        map.put("github", github);

        ProviderConfig facebook = new ProviderConfig();
        facebook.setTokenUrl("https://graph.facebook.com/v18.0/oauth/access_token");
        facebook.setUserInfoUrl("https://graph.facebook.com/v18.0/me?fields=id,name,email,picture");
        map.put("facebook", facebook);

        ProviderConfig apple = new ProviderConfig();
        apple.setTokenUrl("https://appleid.apple.com/auth/token");
        apple.setUserInfoUrl("");
        map.put("apple", apple);

        return map;
    }

    /**
     * 获取提供商配置
     *
     * @param providerCode 提供商 code（如 "google"）
     * @return 配置，不存在则返回 null
     */
    public ProviderConfig getProvider(String providerCode) {
        return providers.get(providerCode.toLowerCase());
    }

    /**
     * 单个 OAuth2 提供商的配置
     */
    @Data
    public static class ProviderConfig {

        /** OAuth2 Client ID */
        private String clientId;

        /** OAuth2 Client Secret */
        private String clientSecret;

        /** 换取 Access Token 的端点 */
        private String tokenUrl;

        /** 获取用户信息的端点 */
        private String userInfoUrl;

        /** 刷新 Access Token 的端点（可选，默认与 tokenUrl 相同） */
        private String refreshTokenUrl;

        /**
         * 是否配置完整（clientId 和 clientSecret 都不为空）
         */
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
