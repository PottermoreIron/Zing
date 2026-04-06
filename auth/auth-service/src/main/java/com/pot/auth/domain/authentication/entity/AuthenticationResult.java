package com.pot.auth.domain.authentication.entity;

import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.Builder;

@Builder
public record AuthenticationResult(
                UserId userId,
                UserDomain userDomain,
                String nickname,
                String email,
                String phone,
                String accessToken,
                String refreshToken,
                Long accessTokenExpiresAt,
                Long refreshTokenExpiresAt,
                LoginContext loginContext) {
}
