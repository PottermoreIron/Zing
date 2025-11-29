package com.pot.auth.domain.port;

import com.pot.auth.domain.oauth2.entity.OAuth2UserInfo;
import com.pot.auth.domain.oauth2.valueobject.OAuth2AuthorizationCode;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;

/**
 * OAuth2端口接口
 *
 * <p>定义OAuth2认证的抽象能力，隔离具体OAuth2提供商实现
 *
 * @author pot
 * @since 2025-11-10
 */
public interface OAuth2Port {

    /**
     * 获取授权URL
     *
     * @param provider    OAuth2提供商
     * @param state       状态参数（防CSRF）
     * @param redirectUri 回调地址
     * @return 授权URL
     */
    String getAuthorizationUrl(OAuth2Provider provider, String state, String redirectUri);

    /**
     * 通过授权码获取用户信息
     *
     * @param provider    OAuth2提供商
     * @param code        授权码
     * @param redirectUri 回调地址
     * @return OAuth2用户信息
     */
    OAuth2UserInfo getUserInfo(
            OAuth2Provider provider,
            OAuth2AuthorizationCode code,
            String redirectUri
    );

    /**
     * 刷新访问令牌
     *
     * @param provider     OAuth2提供商
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    String refreshAccessToken(OAuth2Provider provider, String refreshToken);
}
