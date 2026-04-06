package com.pot.auth.domain.port;

import com.pot.auth.domain.oauth2.entity.OAuth2UserInfo;
import com.pot.auth.domain.oauth2.valueobject.OAuth2AuthorizationCode;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;

public interface OAuth2Port {

        String getAuthorizationUrl(OAuth2Provider provider, String state, String redirectUri);

        OAuth2UserInfo getUserInfo(
            OAuth2Provider provider,
            OAuth2AuthorizationCode code,
            String redirectUri
    );

        String refreshAccessToken(OAuth2Provider provider, String refreshToken);
}
