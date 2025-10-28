package com.pot.auth.service.service.v1;

import com.pot.auth.service.dto.v1.oAuth2.OAuthProviderInfo;
import com.pot.auth.service.dto.v1.response.AuthorizationUrlResponse;
import com.pot.auth.service.dto.v1.session.AuthSession;

import java.util.List;

/**
 * @author: Pot
 * @created: 2025/10/25 23:49
 * @description: Oauth提供商服务接口
 */
public interface OAuthProviderService {
    List<OAuthProviderInfo> listProviders();

    OAuthProviderInfo getProviderInfo(String provider);

    AuthorizationUrlResponse getAuthorizationUrl(String provider, String redirectUri);

    AuthSession handleCallback(String provider, String code, String state);
}
