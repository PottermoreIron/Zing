package com.pot.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Configuration properties for OAuth2 providers.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.oauth2")
public class OAuth2ProviderProperties {

    private boolean enabled = false;

    /**
     * Provider-specific settings keyed by provider code.
     */
    private Map<String, ProviderConfig> providers = defaultProviders();

    private static Map<String, ProviderConfig> defaultProviders() {
        Map<String, ProviderConfig> map = new HashMap<>();

        ProviderConfig google = new ProviderConfig();
        google.setAuthorizationUrl("https://accounts.google.com/o/oauth2/v2/auth");
        google.setScope("openid email profile");
        google.setTokenUrl("https://oauth2.googleapis.com/token");
        google.setUserInfoUrl("https://www.googleapis.com/oauth2/v3/userinfo");
        google.setRefreshTokenUrl("https://oauth2.googleapis.com/token");
        map.put("google", google);

        ProviderConfig github = new ProviderConfig();
        github.setAuthorizationUrl("https://github.com/login/oauth/authorize");
        github.setScope("user:email");
        github.setTokenUrl("https://github.com/login/oauth/access_token");
        github.setUserInfoUrl("https://api.github.com/user");
        map.put("github", github);

        ProviderConfig facebook = new ProviderConfig();
        facebook.setAuthorizationUrl("https://www.facebook.com/v18.0/dialog/oauth");
        facebook.setScope("email,public_profile");
        facebook.setTokenUrl("https://graph.facebook.com/v18.0/oauth/access_token");
        facebook.setUserInfoUrl("https://graph.facebook.com/v18.0/me?fields=id,name,email,picture");
        map.put("facebook", facebook);

        ProviderConfig apple = new ProviderConfig();
        apple.setAuthorizationUrl("https://appleid.apple.com/auth/authorize");
        apple.setScope("name email");
        apple.setExtraAuthParams(Map.of("response_mode", "form_post"));
        apple.setTokenUrl("https://appleid.apple.com/auth/token");
        apple.setUserInfoUrl("");
        map.put("apple", apple);

        return map;
    }

    /**
     * Returns the configuration for the given provider code.
     */
    public ProviderConfig getProvider(String providerCode) {
        return providers.get(providerCode.toLowerCase());
    }

    /**
     * Configuration for a single OAuth2 provider.
     */
    @Getter
    @Setter
    public static class ProviderConfig {

        /** OAuth2 Client ID */
        private String clientId;

        /** OAuth2 Client Secret */
        private String clientSecret;

        /** Authorization endpoint used to redirect the user for consent. */
        private String authorizationUrl;

        /** Space- or comma-separated scopes requested during authorization. */
        private String scope;

        /**
         * Additional query parameters appended to the authorization URL (e.g.
         * response_mode).
         */
        private Map<String, String> extraAuthParams = new LinkedHashMap<>();

        /** Access-token endpoint. */
        private String tokenUrl;

        /** User-info endpoint. */
        private String userInfoUrl;

        /** Optional refresh-token endpoint. */
        private String refreshTokenUrl;

        /**
         * Indicates whether client credentials are present.
         */
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
