package com.pot.auth.domain.port;

import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.RefreshToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import com.pot.auth.domain.authorization.valueobject.PermissionCacheMetadata;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

import java.util.Set;

public interface TokenManagementPort {

        TokenPair generateTokenPair(
            UserId userId,
            UserDomain userDomain,
            String nickname,
            Set<String> authorities,
            PermissionCacheMetadata metadata);

        JwtToken parseAccessToken(String tokenString);

        RefreshToken parseRefreshToken(String tokenString);
}