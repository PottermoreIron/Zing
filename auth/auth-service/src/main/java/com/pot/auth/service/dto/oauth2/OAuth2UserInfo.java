package com.pot.auth.service.dto.oauth2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2用户信息统一抽象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2UserInfo {

    /**
     * 第三方平台用户ID
     */
    private String openId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 个人主页
     */
    private String profileUrl;

    /**
     * OAuth2提供商
     */
    private String provider;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 原始响应数据（JSON格式）
     */
    private String rawData;

    // ==================== 微信特有字段 ====================

    /**
     * 微信UnionID（用于同一开放平台下的应用账号统一）
     */
    private String unionId;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 语言
     */
    private String language;
}

