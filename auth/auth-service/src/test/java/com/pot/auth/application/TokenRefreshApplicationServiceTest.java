package com.pot.auth.application;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.TokenRefreshApplicationService;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * TokenRefreshApplicationService 单元测试
 *
 * <p>
 * 验证：
 * <ul>
 * <li>refreshToken：委托给JwtTokenService，并将TokenPair映射为LoginResponse</li>
 * <li>validateAccessToken：直接委托并返回JwtToken</li>
 * <li>logout：先验证AccessToken，再调用addToBlacklist</li>
 * </ul>
 *
 * @author pot
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRefreshApplicationService 单元测试")
class TokenRefreshApplicationServiceTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private TokenRefreshApplicationService service;

    // ================================================================
    // refreshToken
    // ================================================================

    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        @Test
        @DisplayName("有效的RefreshToken字符串，返回正确的LoginResponse")
        void whenValidRefreshToken_thenReturnLoginResponse() {
            // given
            TokenPair tokenPair = TestFixtures.validTokenPair();
            when(jwtTokenService.refreshToken(TestFixtures.FAKE_REFRESH_TOKEN))
                    .thenReturn(tokenPair);

            // when
            LoginResponse response = service.refreshToken(TestFixtures.FAKE_REFRESH_TOKEN);

            // then
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(TestFixtures.USER_ID.value());
            assertThat(response.userDomain()).isEqualTo(TestFixtures.USER_DOMAIN.name());
            assertThat(response.nickname()).isEqualTo(TestFixtures.USERNAME);
            assertThat(response.accessToken()).isEqualTo(TestFixtures.FAKE_ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(TestFixtures.FAKE_REFRESH_TOKEN);
            // email 和 phone 在 refreshToken 场景下为 null
            assertThat(response.email()).isNull();
            assertThat(response.phoneNumber()).isNull();

            verify(jwtTokenService).refreshToken(TestFixtures.FAKE_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("JwtTokenService 抛出 DomainException，异常向上传播")
        void whenJwtServiceThrows_thenPropagateException() {
            // given
            when(jwtTokenService.refreshToken(any()))
                    .thenThrow(new DomainException(AuthResultCode.TOKEN_EXPIRED));

            // when & then
            assertThatThrownBy(() -> service.refreshToken("expired.token"))
                    .isInstanceOf(DomainException.class);
        }
    }

    // ================================================================
    // validateAccessToken
    // ================================================================

    @Nested
    @DisplayName("validateAccessToken()")
    class ValidateAccessToken {

        @Test
        @DisplayName("有效Token，直接委托给JwtTokenService并返回JwtToken")
        void whenValidToken_thenDelegateAndReturn() {
            // given
            JwtToken accessToken = TestFixtures.validAccessToken();
            when(jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .thenReturn(accessToken);

            // when
            JwtToken result = service.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN);

            // then
            assertThat(result).isSameAs(accessToken);
            verify(jwtTokenService).validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("Token已过期，抛出DomainException")
        void whenExpiredToken_thenThrowDomainException() {
            // given
            when(jwtTokenService.validateAccessToken(any()))
                    .thenThrow(new DomainException(AuthResultCode.TOKEN_EXPIRED));

            // when & then
            assertThatThrownBy(() -> service.validateAccessToken("expired.token"))
                    .isInstanceOf(DomainException.class);
        }
    }

    // ================================================================
    // logout
    // ================================================================

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("有效AccessToken，先验证Token，再将Token加入黑名单")
        void whenValidAccessToken_thenValidateAndBlacklist() {
            // given
            JwtToken accessToken = TestFixtures.validAccessToken();
            when(jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .thenReturn(accessToken);

            // when
            service.logout(TestFixtures.FAKE_ACCESS_TOKEN);

            // then: 验证顺序 - 先validateAccessToken，再addToBlacklist
            verify(jwtTokenService).validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN);
            verify(jwtTokenService).addToBlacklist(
                    eq(accessToken.tokenId()),
                    anyLong());
        }

        @Test
        @DisplayName("AccessToken验证失败，不调用addToBlacklist，异常上传")
        void whenInvalidToken_thenNoBlacklistAndPropagateException() {
            // given
            when(jwtTokenService.validateAccessToken(any()))
                    .thenThrow(new DomainException(AuthResultCode.TOKEN_INVALID));

            // when & then
            assertThatThrownBy(() -> service.logout("invalid.token"))
                    .isInstanceOf(DomainException.class);

            verify(jwtTokenService, never()).addToBlacklist(any(), anyLong());
        }
    }
}
