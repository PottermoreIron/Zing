package com.pot.auth.service.dto.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2 Token响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2TokenResponse {

    /**
     * 访问令牌
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Token类型
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * 过期时间（秒）
     */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * 刷新令牌
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 权限范围
     */
    private String scope;

    // ==================== 微信特有字段 ====================

    /**
     * 微信用户唯一标识（针对当前应用）
     */
    @JsonProperty("openid")
    private String openId;

    /**
     * 微信用户统一标识（针对同一开放平台下的所有应用）
     */
    @JsonProperty("unionid")
    private String unionId;
}

