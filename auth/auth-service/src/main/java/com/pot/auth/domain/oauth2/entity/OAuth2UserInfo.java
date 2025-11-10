package com.pot.auth.domain.oauth2.entity;

import com.pot.auth.domain.oauth2.valueobject.OAuth2OpenId;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import lombok.Builder;
import lombok.Getter;

/**
 * OAuth2用户信息实体
 *
 * <p>封装从OAuth2提供商获取的用户信息
 *
 * @author yecao
 * @since 2025-11-10
 */
@Getter
@Builder
public class OAuth2UserInfo {

    /**
     * OAuth2提供商
     */
    private final OAuth2Provider provider;

    /**
     * OAuth2用户唯一标识（OpenID）
     */
    private final OAuth2OpenId openId;

    /**
     * 用户名
     */
    private final String username;

    /**
     * 邮箱（可能为空）
     */
    private final String email;

    /**
     * 邮箱是否已验证
     */
    private final Boolean emailVerified;

    /**
     * 昵称
     */
    private final String nickname;

    /**
     * 头像URL
     */
    private final String avatarUrl;

    /**
     * 访问令牌（用于调用OAuth2提供商API）
     */
    private final String accessToken;

    /**
     * 刷新令牌
     */
    private final String refreshToken;

    /**
     * 令牌过期时间（秒）
     */
    private final Long expiresIn;

    /**
     * 原始用户数据（JSON格式）
     */
    private final String rawData;
}

