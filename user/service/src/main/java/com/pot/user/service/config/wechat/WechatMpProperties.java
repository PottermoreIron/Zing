package com.pot.user.service.config.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author: Pot
 * @created: 2025/4/13 14:51
 * @description: 微信公众号配置类
 */
@Data
@ConfigurationProperties(prefix = "wechat.mp")
public class WechatMpProperties {
    private String callbackUrl;
    private List<MpConfig> configs;

    @Data
    public static class MpConfig {

        /**
         * 设置微信公众号的appid
         */
        private String appId;

        /**
         * 设置微信公众号的app secret
         */
        private String secret;

        /**
         * 设置微信公众号的token
         */
        private String token;

        /**
         * 设置微信公众号的EncodingAESKey
         */
        private String aesKey;
    }
}
