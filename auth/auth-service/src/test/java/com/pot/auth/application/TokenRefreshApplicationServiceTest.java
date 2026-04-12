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

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRefreshApplicationService unit test")
class TokenRefreshApplicationServiceTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private TokenRefreshApplicationService service;

    // refreshToken

    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        @Test
        @DisplayName("Valid RefreshToken string returns correct LoginResponse")
        void whenValidRefreshToken_thenReturnLoginResponse() {
            TokenPair tokenPair = TestFixtures.validTokenPair();
            when(jwtTokenService.refreshToken(TestFixtures.FAKE_REFRESH_TOKEN))
                    .thenReturn(tokenPair);

            LoginResponse response = service.refreshToken(TestFixtures.FAKE_REFRESH_TOKEN);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(TestFixtures.USER_ID.value());
            assertThat(response.userDomain()).isEqualTo(TestFixtures.USER_DOMAIN.name());
            assertThat(response.nickname()).isEqualTo(TestFixtures.USERNAME);
            assertThat(response.accessToken()).isEqualTo(TestFixtures.FAKE_ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(TestFixtures.FAKE_REFRESH_TOKEN);
            // Refresh-token responses do not carry email or phone fields.
            assertThat(response.email()).isNull();
            assertThat(response.phoneNumber()).isNull();

            verify(jwtTokenService).refreshToken(TestFixtures.FAKE_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("JwtTokenService throws DomainException which propagates upward")
        void whenJwtServiceThrows_thenPropagateException() {
            when(jwtTokenService.refreshToken(any()))
                    .thenThrow(new DomainException(AuthResultCode.TOKEN_EXPIRED));

            assertThatThrownBy(() -> service.refreshToken("expired.token"))
                    .isInstanceOf(DomainException.class);
        }
    }

    // validateAccessToken

    @Nested
    @DisplayName("validateAccessToken()")
    class ValidateAccessToken {

        @Test
        @DisplayName("Valid token delegates directly to JwtTokenService and returns JwtToken")
        void whenValidToken_thenDelegateAndReturn() {
            JwtToken accessToken = TestFixtures.validAccessToken();
            when(jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .thenReturn(accessToken);

            JwtToken result = service.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN);

            assertThat(result).isSameAs(accessToken);
            verify(jwtTokenService).validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("Expired token throws DomainException")
        void whenExpiredToken_thenThrowDomainException() {
            when(jwtTokenService.validateAccessToken(any()))
                    .thenThrow(new DomainException(AuthResultCode.TOKEN_EXPIRED));

            assertThatThrownBy(() -> service.validateAccessToken("expired.token"))
                    .isInstanceOf(DomainException.class);
        }
    }

    // logout

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("Valid AccessToken: token validated then blacklisted")
        void whenValidAccessToken_thenValidateAndBlacklist() {
            JwtToken accessToken = TestFixtures.validAccessToken();
            when(jwtTokenService.validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN))
                    .thenReturn(accessToken);

            service.logout(TestFixtures.FAKE_ACCESS_TOKEN);

            // The service must validate the token before blacklisting it.
            verify(jwtTokenService).validateAccessToken(TestFixtures.FAKE_ACCESS_TOKEN);
            verify(jwtTokenService).addToBlacklist(
                    eq(accessToken.tokenId()),
                    anyLong());
        }

        @Test
        @DisplayName("AccessToken validation failure does not call addToBlacklist and propagates exception")
        void whenInvalidToken_thenNoBlacklistAndPropagateException() {
            when(jwtTokenService.validateAccessToken(any()))
                    .thenThrow(new DomainException(AuthResultCode.TOKEN_INVALID));

            assertThatThrownBy(() -> service.logout("invalid.token"))
                    .isInstanceOf(DomainException.class);

            verify(jwtTokenService, never()).addToBlacklist(any(), anyLong());
        }
    }
}
