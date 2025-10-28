package com.pot.auth.service.oauth2;

import com.pot.auth.service.dto.oauth2.OAuth2TokenResponse;
import com.pot.auth.service.dto.oauth2.OAuth2UserInfo;
import com.pot.auth.service.enums.OAuth2Provider;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2客户端服务接口 - 定义OAuth2标准流程
 */
public interface OAuth2ClientService {

    /**
     * 获取支持的OAuth2提供商
     *
     * @return OAuth2Provider
     */
    OAuth2Provider getProvider();

    /**
     * 生成授权URL
     *
     * @param state 防CSRF攻击的state参数
     * @return 授权URL
     */
    String getAuthorizationUrl(String state);

    /**
     * 使用授权码换取访问令牌
     *
     * @param code 授权码
     * @return OAuth2TokenResponse
     */
    OAuth2TokenResponse exchangeToken(String code);

    /**
     * 获取用户信息
     *
     * @param accessToken 访问令牌
     * @return OAuth2UserInfo
     */
    OAuth2UserInfo getUserInfo(String accessToken);

    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return OAuth2TokenResponse
     */
    default OAuth2TokenResponse refreshAccessToken(String refreshToken) {
        throw new UnsupportedOperationException("该提供商不支持刷新令牌");
    }
}

