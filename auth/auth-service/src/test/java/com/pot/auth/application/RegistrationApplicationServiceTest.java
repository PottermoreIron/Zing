package com.pot.auth.application;

import com.pot.auth.application.command.RegisterCommand;
import com.pot.auth.application.command.RegisterRequestCommand;
import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.application.service.OneStopAuthenticationService;
import com.pot.auth.application.service.RegistrationApplicationService;
import com.pot.auth.application.strategy.RegisterStrategy;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.strategy.factory.RegisterStrategyFactory;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationApplicationService unit test")
class RegistrationApplicationServiceTest {

        private static final UserId USER_ID = UserId.of(10001L);
        private static final UserDomain USER_DOMAIN = UserDomain.MEMBER;
        private static final String USERNAME = "test_user";
        private static final String PASSWORD = "Password123!";
        private static final String EMAIL = "test@example.com";
        private static final String PHONE = "+8613800138000";
        private static final String ACCESS_TOKEN = "fake.access.token";
        private static final String REFRESH_TOKEN = "fake.refresh.token";

        @Mock
        private RegisterStrategyFactory registerStrategyFactory;

        @Mock
        private ValidationChain<RegistrationContext> registrationValidationChain;

        @Mock
        private OneStopAuthenticationService oneStopAuthenticationService;

        @InjectMocks
        private RegistrationApplicationService service;

        @Nested
        @DisplayName("Traditional registration (username-password)")
        class TraditionalRegister {

                @Test
                @DisplayName("Username-password registration: delegates to strategy and returns correct RegisterResponse fields")
                void whenUsernamePasswordRegister_thenDelegateAndReturnResponse() {
                        RegisterCommand command = new RegisterRequestCommand(
                                        RegisterType.USERNAME_PASSWORD,
                                        USER_DOMAIN,
                                        USERNAME,
                                        null,
                                        null,
                                        PASSWORD,
                                        null,
                                        null,
                                        null,
                                        null);

                        AuthenticationResult authResult = authResult();
                        RegisterStrategy mockStrategy = mock(RegisterStrategy.class);
                        doReturn(mockStrategy).when(registerStrategyFactory)
                                        .getStrategy(RegisterType.USERNAME_PASSWORD);
                        when(mockStrategy.execute(any(RegistrationContext.class))).thenReturn(authResult);

                        RegisterResponse response = service.register(command, "127.0.0.1", "Mozilla/5.0");

                        assertThat(response).isNotNull();
                        assertThat(response.userId()).isEqualTo(USER_ID.value());
                        assertThat(response.userDomain()).isEqualTo(USER_DOMAIN.name());
                        assertThat(response.nickname()).isEqualTo(USERNAME);
                        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
                        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
                        assertThat(response.message()).isEqualTo("Registration successful");

                        verify(registerStrategyFactory).getStrategy(RegisterType.USERNAME_PASSWORD);
                        verify(registrationValidationChain).validate(any(RegistrationContext.class));
                        verify(mockStrategy).execute(any(RegistrationContext.class));
                }

                @Test
                @DisplayName("Null userAgent uses default value 'Unknown' without throwing NPE")
                void whenUserAgentNull_thenUseDefaultValue() {
                        RegisterCommand command = new RegisterRequestCommand(
                                        RegisterType.USERNAME_PASSWORD,
                                        USER_DOMAIN,
                                        USERNAME,
                                        null,
                                        null,
                                        PASSWORD,
                                        null,
                                        null,
                                        null,
                                        null);

                        RegisterStrategy mockStrategy = mock(RegisterStrategy.class);
                        doReturn(mockStrategy).when(registerStrategyFactory).getStrategy(any());
                        when(mockStrategy.execute(any())).thenReturn(authResult());

                        service.register(command, "127.0.0.1", null);
                        verify(mockStrategy).execute(any());
                }

                @Test
                @DisplayName("Strategy throws DomainException which propagates upward")
                void whenStrategyThrows_thenPropagateException() {
                        RegisterCommand command = new RegisterRequestCommand(
                                        RegisterType.USERNAME_PASSWORD,
                                        USER_DOMAIN,
                                        USERNAME,
                                        null,
                                        null,
                                        PASSWORD,
                                        null,
                                        null,
                                        null,
                                        null);

                        RegisterStrategy mockStrategy = mock(RegisterStrategy.class);
                        doReturn(mockStrategy).when(registerStrategyFactory).getStrategy(any());
                        when(mockStrategy.execute(any()))
                                        .thenThrow(new DomainException(AuthResultCode.USERNAME_ALREADY_EXISTS));

                        assertThatThrownBy(() -> service.register(command, "127.0.0.1", "UA"))
                                        .isInstanceOf(DomainException.class);
                }
        }

        @Nested
        @DisplayName("OAuth2 registration (delegated to OneStopAuthenticationService)")
        class OAuth2Register {

                @Test
                @DisplayName("OAuth2 registration: delegates to OneStopAuth and converts response to RegisterResponse")
                void whenOAuth2Register_thenDelegateToOneStopAuth() {
                        RegisterCommand command = new RegisterRequestCommand(
                                        RegisterType.OAUTH2,
                                        USER_DOMAIN,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "auth_code_123",
                                        "state_xyz",
                                        "GOOGLE");

                        long now = System.currentTimeMillis() / 1000;
                        OneStopAuthResponse authResponse = OneStopAuthResponse.builder()
                                        .userId(USER_ID)
                                        .userDomain(USER_DOMAIN)
                                        .nickname(USERNAME)
                                        .email(EMAIL)
                                        .phone(PHONE)
                                        .accessToken(ACCESS_TOKEN)
                                        .refreshToken(REFRESH_TOKEN)
                                        .accessTokenExpiresAt(now + 3600)
                                        .refreshTokenExpiresAt(now + 2592000)
                                        .build();

                        when(oneStopAuthenticationService.authenticate(any(), any(), any()))
                                        .thenReturn(authResponse);

                        RegisterResponse response = service.register(command, "127.0.0.1", "UA");

                        assertThat(response).isNotNull();
                        assertThat(response.userId()).isEqualTo(USER_ID.value());
                        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);

                        verify(oneStopAuthenticationService).authenticate(any(), eq("127.0.0.1"), eq("UA"));
                        verifyNoInteractions(registerStrategyFactory, registrationValidationChain);
                }
        }

        @Nested
        @DisplayName("WeChat registration (delegated to OneStopAuthenticationService)")
        class WeChatRegister {

                @Test
                @DisplayName("WeChat registration: delegates to OneStopAuth and converts response to RegisterResponse")
                void whenWeChatRegister_thenDelegateToOneStopAuth() {
                        RegisterCommand command = new RegisterRequestCommand(
                                        RegisterType.WECHAT,
                                        USER_DOMAIN,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "wechat_code_abc",
                                        "wechat_state",
                                        null);

                        long now = System.currentTimeMillis() / 1000;
                        OneStopAuthResponse authResponse = OneStopAuthResponse.builder()
                                        .userId(USER_ID)
                                        .userDomain(USER_DOMAIN)
                                        .nickname(USERNAME)
                                        .email(null)
                                        .phone(PHONE)
                                        .accessToken(ACCESS_TOKEN)
                                        .refreshToken(REFRESH_TOKEN)
                                        .accessTokenExpiresAt(now + 3600)
                                        .refreshTokenExpiresAt(now + 2592000)
                                        .build();

                        when(oneStopAuthenticationService.authenticate(any(), any(), any()))
                                        .thenReturn(authResponse);

                        RegisterResponse response = service.register(command, "10.0.0.1", "WeChat/8.0");

                        assertThat(response).isNotNull();
                        assertThat(response.userId()).isEqualTo(USER_ID.value());
                        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);

                        verify(oneStopAuthenticationService).authenticate(any(), eq("10.0.0.1"), eq("WeChat/8.0"));
                        verifyNoInteractions(registerStrategyFactory, registrationValidationChain);
                }
        }

        private AuthenticationResult authResult() {
                long now = System.currentTimeMillis() / 1000;
                return AuthenticationResult.builder()
                                .userId(USER_ID)
                                .userDomain(USER_DOMAIN)
                                .nickname(USERNAME)
                                .email(EMAIL)
                                .phone(PHONE)
                                .accessToken(ACCESS_TOKEN)
                                .refreshToken(REFRESH_TOKEN)
                                .accessTokenExpiresAt(now + 3600)
                                .refreshTokenExpiresAt(now + 2592000)
                                .build();
        }
}
