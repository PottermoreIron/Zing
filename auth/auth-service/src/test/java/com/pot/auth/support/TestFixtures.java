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

/**
 * 测试数据工厂（Object Mother模式）
 *
 * <p>
 * 提供常用测试对象的静态工厂方法，统一管理测试数据，避免重复代码。
 *
 * @author pot
 */
public final class TestFixtures {

    // ============================================================
    // 常量
    // ============================================================

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
        // 工具类，不允许实例化
    }

    // ============================================================
    // UserDTO
    // ============================================================

    /**
     * 创建标准的Member用户DTO
     */
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

    // ============================================================
    // JwtToken
    // ============================================================

    /**
     * 创建未过期的AccessToken（1小时后过期）
     */
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

    /**
     * 创建已过期的AccessToken
     */
    public static JwtToken expiredAccessToken() {
        long now = System.currentTimeMillis() / 1000;
        return new JwtToken(
                ACCESS_TOKEN_ID,
                USER_ID,
                USER_DOMAIN,
                USERNAME,
                PERMISSIONS,
                now - 7200,
                now - 3600, // 1小时前已过期
                FAKE_ACCESS_TOKEN,
                Map.of("perm_version", 1L));
    }

    // ============================================================
    // RefreshToken
    // ============================================================

    /**
     * 创建未过期的RefreshToken（30天后过期）
     */
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

    /**
     * 创建已过期的RefreshToken
     */
    public static RefreshToken expiredRefreshToken() {
        long now = System.currentTimeMillis() / 1000;
        return new RefreshToken(
                REFRESH_TOKEN_ID,
                USER_ID,
                USER_DOMAIN,
                DeviceId.of(1L),
                now - 2592000,
                now - 86400, // 1天前已过期
                FAKE_REFRESH_TOKEN);
    }

    // ============================================================
    // TokenPair
    // ============================================================

    /**
     * 创建有效的TokenPair
     */
    public static TokenPair validTokenPair() {
        return new TokenPair(validAccessToken(), validRefreshToken());
    }

    // ============================================================
    // PermissionCacheMetadata
    // ============================================================

    public static PermissionCacheMetadata permCacheMetadata() {
        return PermissionCacheMetadata.empty(1L);
    }

    // ============================================================
    // AuthenticationResult
    // ============================================================

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

    // ============================================================
    // LoginResponse
    // ============================================================

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

    // ============================================================
    // 请求对象
    // ============================================================

    /**
     * 创建用户名密码登录请求
     */
    public static UsernamePasswordLoginRequest usernamePasswordRequest() {
        return new UsernamePasswordLoginRequest(
                LoginType.USERNAME_PASSWORD,
                USERNAME,
                PASSWORD,
                USER_DOMAIN);
    }
}
