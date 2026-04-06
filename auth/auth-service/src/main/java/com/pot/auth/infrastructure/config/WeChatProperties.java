package com.pot.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for WeChat login.
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.wechat")
public class WeChatProperties {

    private boolean enabled = false;

    private String appId;

    private String appSecret;

    /**
     * Endpoint used to exchange an authorization code for an access token.
     */
    private String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";

    /**
     * Endpoint used to load the user profile.
     */
    private String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo";

    /**
     * Endpoint used to refresh an access token.
     */
    private String refreshTokenUrl = "https://api.weixin.qq.com/sns/oauth2/refresh_token";

    /**
     * Endpoint used to validate an access token.
     */
    private String authCheckUrl = "https://api.weixin.qq.com/sns/auth";

    /**
     * Indicates whether the required credentials are configured.
     */
    public boolean isConfigured() {
        return appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }
}
