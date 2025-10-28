package com.pot.auth.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2提供商枚举
 */
@Getter
@AllArgsConstructor
public enum OAuth2Provider {

    GITHUB("github", "GitHub", LoginType.OAUTH2_GITHUB),
    GOOGLE("google", "Google", LoginType.OAUTH2_GOOGLE),
    WECHAT("wechat", "微信", LoginType.OAUTH2_WECHAT),
    FACEBOOK("facebook", "Facebook", LoginType.OAUTH2_FACEBOOK),
    TWITTER("twitter", "Twitter", LoginType.OAUTH2_TWITTER);

    /**
     * 提供商唯一标识
     */
    private final String provider;

    /**
     * 提供商显示名称
     */
    private final String displayName;

    /**
     * 对应的登录类型
     */
    private final LoginType loginType;

    /**
     * 根据provider获取枚举
     */
    public static OAuth2Provider fromProvider(String provider) {
        for (OAuth2Provider value : values()) {
            if (value.getProvider().equalsIgnoreCase(provider)) {
                return value;
            }
        }
        throw new IllegalArgumentException("不支持的OAuth2提供商: " + provider);
    }

    /**
     * 根据LoginType获取OAuth2Provider
     */
    public static OAuth2Provider fromLoginType(LoginType loginType) {
        for (OAuth2Provider value : values()) {
            if (value.getLoginType() == loginType) {
                return value;
            }
        }
        throw new IllegalArgumentException("该登录类型不是OAuth2类型: " + loginType);
    }
}

