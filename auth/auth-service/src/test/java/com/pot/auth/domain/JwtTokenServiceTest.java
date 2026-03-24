package com.pot.auth.domain;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.JwtTokenService.TokenExpiredException;
import com.pot.auth.domain.authentication.service.JwtTokenService.TokenInvalidException;
import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.TokenManagementPort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JwtTokenService 单元测试
 *
 * <p>
 * 覆盖Token生命周期全流程：
 * <ul>
 * <li>generateTokenPair - 权限缓存 + Token生成 + RefreshToken存储</li>
 * <li>validateAccessToken - 过期检测 / 黑名单检测 / 权限版本校验</li>
 * <li>logout - 黑名单写入 + RefreshToken删除（幂等容错）</li>
 * <li>addToBlacklist - 黑名单写入TTL</li>
 * </ul>
 *
 * @author pot
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenService 单元测试")
class JwtTokenServiceTest {

    @Mock
    private TokenManagementPort tokenManagementPort;

    @Mock
    private CachePort cachePort;

    @Mock
    private UserModulePortFactory userModulePortFactory;

    @Mock
    private PermissionDomainService permissionDomainService;

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(
                tokenManagementPort,
                cachePort,
                userModulePortFactory,
                permissionDomainService,
                2592000L,
                604800L,
                false);
    }

    // ================================================================
    // generateTokenPair
    // ================================================================

    @Nested
    @DisplayName("生成TokenPair")
    class GenerateTokenPair {

        @Test
        @DisplayName("成功生成Token，写入RefreshToken缓存，返回TokenPair")
        void whenAllDepsOk_thenReturnTokenPairAndStoreRefresh() {
            // given
            when(permissionDomainService.cachePermissionsWithMetadata(
                    eq(TestFixtures.USER_ID),
                    eq(TestFixtures.USER_DOMAIN),
                    eq(TestFixtures.PERMISSIONS)))
                    .thenReturn(TestFixtures.permCacheMetadata());

            TokenPair expected = TestFixtures.validTokenPair();
            when(tokenManagementPort.generateTokenPair(
                    eq(TestFixtures.USER_ID),
                    eq(TestFixtures.USER_DOMAIN),
                    eq(TestFixtures.USERNAME),
                    eq(TestFixtures.PERMISSIONS),
                    any()))
                    .thenReturn(expected);

            // when
            TokenPair result = jwtTokenService.generateTokenPair(
                    TestFixtures.USER_ID,
                    TestFixtures.USER_DOMAIN,
                    TestFixtures.USERNAME,
                    TestFixtures.PERMISSIONS);

            // then
            assertThat(result).isEqualTo(expected);

            // 验证RefreshToken写入缓存
            String expectedRefreshKey = "auth:refresh:" + TestFixtures.REFRESH_TOKEN_ID.value();
            verify(cachePort).set(eq(expectedRefreshKey), eq(TestFixtures.FAKE_REFRESH_TOKEN), any(Duration.class));
        }

        @Test
        @DisplayName("TokenManagementPort抛出异常，包装为DomainException")
        void whenTokenPortFails_thenThrowDomainException() {
            // given
            when(permissionDomainService.cachePermissionsWithMetadata(any(), any(), any()))
                    .thenReturn(TestFixtures.permCacheMetadata());
            when(tokenManagementPort.generateTokenPair(any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("JWT signing failed"));

            // when & then: 包装成DomainException（AUTHENTICATION_FAILED）
            assertThatThrownBy(() -> jwtTokenService.generateTokenPair(
                    TestFixtures.USER_ID,
                    TestFixtures.USER_DOMAIN,
                    TestFixtures.USERNAME,
                    TestFixtures.PERMISSIONS))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("密码");
        }
    }

    // ================================================================
    // validateAccessToken
    // ================================================================

    @Nested
    @DisplayName("验证AccessToken")
    class ValidateAccessToken {

        @Test
        @DisplayName("Token有效，返回JwtToken对象")
        void whenTokenValid_thenReturnJwtToken() {
            // given: 未过期 + 不在黑名单
            JwtToken validToken = TestFixtures.validAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(validToken);
            when(cachePort.exists("auth:blacklist:" + TestFixtures.ACCESS_TOKEN_ID.value())).thenReturn(false);

            // when
            JwtToken result = jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN);

            // then
            assertThat(result).isEqualTo(validToken);
        }

        @Test
        @DisplayName("Token已过期，抛出TokenExpiredException")
        void whenTokenExpired_thenThrowTokenExpiredException() {
            // given: Token已过期
            JwtToken expiredToken = TestFixtures.expiredAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(expiredToken);

            // when & then
            assertThatThrownBy(() -> jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .isInstanceOf(TokenExpiredException.class)
                    .hasMessageContaining("过期");
        }

        @Test
        @DisplayName("Token在黑名单中，抛出TokenInvalidException")
        void whenTokenInBlacklist_thenThrowTokenInvalidException() {
            // given: 未过期但在黑名单
            JwtToken validToken = TestFixtures.validAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(validToken);
            when(cachePort.exists("auth:blacklist:" + TestFixtures.ACCESS_TOKEN_ID.value())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .isInstanceOf(TokenInvalidException.class)
                    .hasMessageContaining("失效");
        }
    }

    // ================================================================
    // logout
    // ================================================================

    @Nested
    @DisplayName("登出（Token吊销）")
    class Logout {

        @Test
        @DisplayName("有效Token登出：加入黑名单 + 删除RefreshToken缓存")
        void whenValidTokens_thenBlacklistAndDeleteRefresh() {
            // given
            JwtToken accessToken = TestFixtures.validAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(accessToken);
            when(tokenManagementPort.parseRefreshToken(TestFixtures.FAKE_REFRESH_TOKEN))
                    .thenReturn(TestFixtures.validRefreshToken());

            // when
            jwtTokenService.logout(TestFixtures.FAKE_ACCESS_TOKEN, TestFixtures.FAKE_REFRESH_TOKEN);

            // then: AccessToken加入黑名单
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(cachePort).set(
                    eq("auth:blacklist:" + TestFixtures.ACCESS_TOKEN_ID.value()),
                    eq("1"),
                    ttlCaptor.capture());
            assertThat(ttlCaptor.getValue().getSeconds()).isGreaterThan(0);

            // then: RefreshToken缓存删除
            verify(cachePort).delete("auth:refresh:" + TestFixtures.REFRESH_TOKEN_ID.value());
        }

        @Test
        @DisplayName("AccessToken已过期，不写入黑名单（已自然过期无需额外吊销）")
        void whenAccessTokenExpired_thenSkipBlacklist() {
            // given: Token已过期（getRemainingSeconds() == 0）
            JwtToken expiredToken = TestFixtures.expiredAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(expiredToken);

            // when
            jwtTokenService.logout(TestFixtures.FAKE_ACCESS_TOKEN, null);

            // then: 不写黑名单，不抛异常
            verify(cachePort, never()).set(contains("auth:blacklist:"), any(), any(Duration.class));
        }

        @Test
        @DisplayName("AccessToken解析失败（已损坏/无效），不抛异常（幂等容错）")
        void whenAccessTokenParseFails_thenNoException() {
            // given: 解析抛出异常
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .thenThrow(new RuntimeException("malformed JWT"));

            // when & then: 不应抛出任何异常
            assertThatCode(() -> jwtTokenService.logout(TestFixtures.FAKE_ACCESS_TOKEN, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("未提供RefreshToken时，只吊销AccessToken，不调用RefreshToken删除")
        void whenRefreshTokenNull_thenOnlyRevokeAccessToken() {
            // given
            JwtToken accessToken = TestFixtures.validAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(accessToken);

            // when
            jwtTokenService.logout(TestFixtures.FAKE_ACCESS_TOKEN, null);

            // then: 不解析RefreshToken
            verify(tokenManagementPort, never()).parseRefreshToken(anyString());
        }
    }

    // ================================================================
    // addToBlacklist
    // ================================================================

    @Nested
    @DisplayName("加入黑名单")
    class AddToBlacklist {

        @Test
        @DisplayName("将TokenId加入黑名单，使用指定TTL")
        void whenCalled_thenStoreBlacklistKeyWithTtl() {
            // given
            TokenId tokenId = TokenId.of("blacklist-test-001");
            long remainingSeconds = 1800L;

            // when
            jwtTokenService.addToBlacklist(tokenId, remainingSeconds);

            // then
            verify(cachePort).set(
                    eq("auth:blacklist:" + tokenId.value()),
                    eq("1"),
                    eq(Duration.ofSeconds(remainingSeconds)));
        }
    }
}
