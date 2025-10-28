package com.pot.auth.service.oauth2.wechat.config;

import com.pot.auth.service.config.OAuth2ClientProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 微信OAuth2配置
 * <p>
 * 注意：本实现暂不使用WxJava的高级特性，而是直接通过HTTP调用微信API
 * 这样可以保持与现有OAuth2架构的一致性，同时减少依赖复杂度
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "oauth2.clients.wechat", name = "enabled", havingValue = "true")
public class WxJavaConfig {

    private final OAuth2ClientProperties oauth2Properties;

    @PostConstruct
    public void init() {
        OAuth2ClientProperties.OAuth2ClientConfig wechatConfig =
                oauth2Properties.getClients().get("wechat");

        if (wechatConfig != null && wechatConfig.getEnabled()) {
            log.info("微信OAuth2登录配置已启用: appId={}", wechatConfig.getClientId());
        }
    }
}

