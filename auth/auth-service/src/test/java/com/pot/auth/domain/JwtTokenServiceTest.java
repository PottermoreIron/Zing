package com.pot.auth.domain;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.shared.exception.DomainException;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenService unit test")
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
                false,
                5);
    }

    // generateTokenPair

    @Nested
    @DisplayName("Generate TokenPair")
    class GenerateTokenPair {

        @Test
        @DisplayName("Successful token generation writes RefreshToken to cache and returns TokenPair")
        void whenAllDepsOk_thenReturnTokenPairAndStoreRefresh() {
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

            TokenPair result = jwtTokenService.generateTokenPair(
                    TestFixtures.USER_ID,
                    TestFixtures.USER_DOMAIN,
                    TestFixtures.USERNAME,
                    TestFixtures.PERMISSIONS);

            assertThat(result).isEqualTo(expected);
            String expectedRefreshKey = "refresh:" + TestFixtures.REFRESH_TOKEN_ID.value();
            verify(cachePort).set(eq(expectedRefreshKey), eq(TestFixtures.FAKE_REFRESH_TOKEN), any(Duration.class));
        }

        @Test
        @DisplayName("TokenManagementPort exception is wrapped as DomainException")
        void whenTokenPortFails_thenThrowDomainException() {
            when(permissionDomainService.cachePermissionsWithMetadata(any(), any(), any()))
                    .thenReturn(TestFixtures.permCacheMetadata());
            when(tokenManagementPort.generateTokenPair(any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("JWT signing failed"));

            assertThatThrownBy(() -> jwtTokenService.generateTokenPair(
                    TestFixtures.USER_ID,
                    TestFixtures.USER_DOMAIN,
                    TestFixtures.USERNAME,
                    TestFixtures.PERMISSIONS))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("password");
        }
    }

    // validateAccessToken

    @Nested
    @DisplayName("Validate AccessToken")
    class ValidateAccessToken {

        @Test
        @DisplayName("Valid token returns JwtToken object")
        void whenTokenValid_thenReturnJwtToken() {
            JwtToken validToken = TestFixtures.validAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(validToken);
            when(cachePort.exists("blacklist:" + TestFixtures.ACCESS_TOKEN_ID.value())).thenReturn(false);

            JwtToken result = jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN);

            assertThat(result).isEqualTo(validToken);
        }

        @Test
        @DisplayName("Expired token throws TokenExpiredException")
        void whenTokenExpired_thenThrowTokenExpiredException() {
            JwtToken expiredToken = TestFixtures.expiredAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(expiredToken);

            assertThatThrownBy(() -> jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Blacklisted token throws TokenInvalidException")
        void whenTokenInBlacklist_thenThrowTokenInvalidException() {
            JwtToken validToken = TestFixtures.validAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(validToken);
            when(cachePort.exists("blacklist:" + TestFixtures.ACCESS_TOKEN_ID.value())).thenReturn(true);

            assertThatThrownBy(() -> jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("revoked");
        }
    }

    // logout

    @Nested
    @DisplayName("Logout (token revocation)")
    class Logout {

        @Test
        @DisplayName("Valid token logout: blacklisted and RefreshToken cache deleted")
        void whenValidTokens_thenBlacklistAndDeleteRefresh() {
            JwtToken accessToken = TestFixtures.validAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(accessToken);
            when(tokenManagementPort.parseRefreshToken(TestFixtures.FAKE_REFRESH_TOKEN))
                    .thenReturn(TestFixtures.validRefreshToken());

            jwtTokenService.logout(TestFixtures.FAKE_ACCESS_TOKEN, TestFixtures.FAKE_REFRESH_TOKEN);

            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(cachePort).set(
                    eq("blacklist:" + TestFixtures.ACCESS_TOKEN_ID.value()),
                    eq("1"),
                    ttlCaptor.capture());
            assertThat(ttlCaptor.getValue().getSeconds()).isGreaterThan(0);
            verify(cachePort).delete("refresh:" + TestFixtures.REFRESH_TOKEN_ID.value());
        }

        @Test
        @DisplayName("Expired AccessToken is not blacklisted (naturally expired, no additional revocation needed)")
        void whenAccessTokenExpired_thenSkipBlacklist() {
            JwtToken expiredToken = TestFixtures.expiredAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(expiredToken);

            jwtTokenService.logout(TestFixtures.FAKE_ACCESS_TOKEN, null);

            verify(cachePort, never()).set(contains("blacklist:"), any(), any(Duration.class));
        }

        @Test
        @DisplayName("Corrupted/invalid AccessToken does not throw exception (idempotent fault tolerance)")
        void whenAccessTokenParseFails_thenNoException() {
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .thenThrow(new RuntimeException("malformed JWT"));

            assertThatCode(() -> jwtTokenService.logout(TestFixtures.FAKE_ACCESS_TOKEN, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Without RefreshToken: only AccessToken is revoked, RefreshToken delete is not called")
        void whenRefreshTokenNull_thenOnlyRevokeAccessToken() {
            JwtToken accessToken = TestFixtures.validAccessToken();
            when(tokenManagementPort.parseAccessToken(TestFixtures.FAKE_ACCESS_TOKEN)).thenReturn(accessToken);

            jwtTokenService.logout(TestFixtures.FAKE_ACCESS_TOKEN, null);

            verify(tokenManagementPort, never()).parseRefreshToken(anyString());
        }
    }

    // addToBlacklist

    @Nested
    @DisplayName("Add to blacklist")
    class AddToBlacklist {

        @Test
        @DisplayName("TokenId is blacklisted with the specified TTL")
        void whenCalled_thenStoreBlacklistKeyWithTtl() {
            TokenId tokenId = TokenId.of("blacklist-test-001");
            long remainingSeconds = 1800L;

            jwtTokenService.addToBlacklist(tokenId, remainingSeconds);

            verify(cachePort).set(
                    eq("blacklist:" + tokenId.value()),
                    eq("1"),
                    eq(Duration.ofSeconds(remainingSeconds)));
        }
    }
}
