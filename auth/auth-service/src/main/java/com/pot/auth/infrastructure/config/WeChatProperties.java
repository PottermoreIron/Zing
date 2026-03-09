package com.pot.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信公众号/开放平台 OAuth2 配置
 *
 * <p>
 * 配置示例（application.yml）：
 * 
 * <pre>
 * auth:
 *   wechat:
 *     enabled: true
 *     app-id: wx_your_app_id
 *     app-secret: your_app_secret
 * </pre>
 *
 * @author pot
 * @since 2025-12-14
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.wechat")
public class WeChatProperties {

    /** 是否启用微信登录（默认关闭） */
    private boolean enabled = false;

    /** 微信公众号/开放平台 AppID */
    private String appId;

    /** 微信公众号/开放平台 AppSecret */
    private String appSecret;

    /**
     * 获取 Access Token 的端点（网页授权）
     * <p>
     * 默认使用微信官方端点，可替换为代理地址
     */
    private String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";

    /**
     * 获取用户信息的端点
     */
    private String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo";

    /**
     * 刷新 Access Token 的端点
     */
    private String refreshTokenUrl = "https://api.weixin.qq.com/sns/oauth2/refresh_token";

    /**
     * 验证 Access Token 的端点
     */
    private String authCheckUrl = "https://api.weixin.qq.com/sns/auth";

    /**
     * 是否配置完整
     */
    public boolean isConfigured() {
        return appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }
}
