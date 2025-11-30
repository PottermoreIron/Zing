package com.pot.auth.domain.oauth2.entity;

import com.pot.auth.domain.oauth2.valueobject.OAuth2OpenId;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import lombok.Builder;

/**
 * OAuth2用户信息实体
 *
 * <p>
 * 封装从OAuth2提供商获取的用户信息
 *
 * <p>
 * 使用 record 确保不可变性和简洁性
 *
 * @param provider      OAuth2提供商
 * @param openId        OAuth2用户唯一标识（OpenID）
 * @param username      用户名
 * @param email         邮箱（可能为空）
 * @param emailVerified 邮箱是否已验证
 * @param nickname      昵称
 * @param avatarUrl     头像URL
 * @param accessToken   访问令牌（用于调用OAuth2提供商API）
 * @param refreshToken  刷新令牌
 * @param expiresIn     令牌过期时间（秒）
 * @param rawData       原始用户数据（JSON格式）
 * @author pot
 * @since 2025-11-10
 */
@Builder
public record OAuth2UserInfo(
        OAuth2Provider provider,
        OAuth2OpenId openId,
        String username,
        String email,
        Boolean emailVerified,
        String nickname,
        String avatarUrl,
        String accessToken,
        String refreshToken,
        Long expiresIn,
        String rawData) {
}
