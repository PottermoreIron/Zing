package com.pot.auth.support;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.RefreshToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import com.pot.auth.domain.authorization.valueobject.PermissionCacheMetadata;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.valueobject.DeviceId;
import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.interfaces.dto.auth.UsernamePasswordLoginRequest;
import com.pot.auth.domain.shared.enums.LoginType;

import java.util.Map;
import java.util.Set;

public final class TestFixtures {

    public static final UserId USER_ID = UserId.of(10001L);
    public static final UserDomain USER_DOMAIN = UserDomain.MEMBER;
    public static final String USERNAME = "test_user";
    public static final String EMAIL = "test@example.com";
    public static final String PHONE = "+8613800138000";
    public static final String PASSWORD = "Password123!";
    public static final String VALID_CODE = "123456";
    public static final Set<String> PERMISSIONS = Set.of("member:read", "member:write");

    public static final String FAKE_ACCESS_TOKEN = "fake.access.token";
    public static final String FAKE_REFRESH_TOKEN = "fake.refresh.token";
    public static final TokenId ACCESS_TOKEN_ID = TokenId.of("acc-test-id-001");
    public static final TokenId REFRESH_TOKEN_ID = TokenId.of("ref-test-id-001");

    private TestFixtures() {
    }

    public static UserDTO memberUserDTO() {
        return UserDTO.builder()
                .userId(USER_ID)
                .userDomain(USER_DOMAIN)
                .nickname(USERNAME)
                .email(EMAIL)
                .phone(PHONE)
                .status("ACTIVE")
                .permissions(PERMISSIONS)
                .build();
    }

    public static JwtToken validAccessToken() {
        long now = System.currentTimeMillis() / 1000;
        return new JwtToken(
                ACCESS_TOKEN_ID,
                USER_ID,
                USER_DOMAIN,
                USERNAME,
                PERMISSIONS,
                now,
                now + 3600,
                FAKE_ACCESS_TOKEN,
                Map.of("perm_version", 1L));
    }

    public static JwtToken expiredAccessToken() {
        long now = System.currentTimeMillis() / 1000;
        return new JwtToken(
                ACCESS_TOKEN_ID,
                USER_ID,
                USER_DOMAIN,
                USERNAME,
                PERMISSIONS,
                now - 7200,
                now - 3600,
                FAKE_ACCESS_TOKEN,
                Map.of("perm_version", 1L));
    }

    public static RefreshToken validRefreshToken() {
        long now = System.currentTimeMillis() / 1000;
        return new RefreshToken(
                REFRESH_TOKEN_ID,
                USER_ID,
                USER_DOMAIN,
                DeviceId.of(1L),
                now,
                now + 2592000,
                FAKE_REFRESH_TOKEN);
    }

    public static RefreshToken expiredRefreshToken() {
        long now = System.currentTimeMillis() / 1000;
        return new RefreshToken(
                REFRESH_TOKEN_ID,
                USER_ID,
                USER_DOMAIN,
                DeviceId.of(1L),
                now - 2592000,
                now - 86400,
                FAKE_REFRESH_TOKEN);
    }

    public static TokenPair validTokenPair() {
        return new TokenPair(validAccessToken(), validRefreshToken());
    }

    public static PermissionCacheMetadata permCacheMetadata() {
        return PermissionCacheMetadata.empty(1L);
    }

    public static AuthenticationResult authResult() {
        long now = System.currentTimeMillis() / 1000;
        return AuthenticationResult.builder()
                .userId(USER_ID)
                .userDomain(USER_DOMAIN)
                .nickname(USERNAME)
                .email(EMAIL)
                .phone(PHONE)
                .accessToken(FAKE_ACCESS_TOKEN)
                .refreshToken(FAKE_REFRESH_TOKEN)
                .accessTokenExpiresAt(now + 3600)
                .refreshTokenExpiresAt(now + 2592000)
                .build();
    }

    public static LoginResponse loginResponse() {
        long now = System.currentTimeMillis() / 1000;
        return new LoginResponse(
                USER_ID.value(),
                USER_DOMAIN.name(),
                USERNAME,
                EMAIL,
                PHONE,
                FAKE_ACCESS_TOKEN,
                FAKE_REFRESH_TOKEN,
                now + 3600,
                now + 2592000);
    }

    public static UsernamePasswordLoginRequest usernamePasswordRequest() {
        return new UsernamePasswordLoginRequest(
                LoginType.USERNAME_PASSWORD,
                USERNAME,
                PASSWORD,
                USER_DOMAIN);
    }
}
