package com.pot.auth.domain.oauth2.entity;

import com.pot.auth.domain.oauth2.valueobject.OAuth2OpenId;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import lombok.Builder;

@Builder
public record OAuth2UserInfo(
                OAuth2Provider provider,
                OAuth2OpenId openId,
                String email,
                Boolean emailVerified,
                String nickname,
                String avatarUrl,
                String accessToken,
                String refreshToken,
                Long expiresIn,
                String rawData) {
}
