package com.pot.auth.application;

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
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.interfaces.dto.register.OAuth2RegisterRequest;
import com.pot.auth.interfaces.dto.register.UsernamePasswordRegisterRequest;
import com.pot.auth.interfaces.dto.register.WeChatRegisterRequest;
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

/**
 * Unit tests for RegistrationApplicationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationApplicationService 单元测试")
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
        @DisplayName("传统注册（用户名密码）")
        class TraditionalRegister {

                @Test
                @DisplayName("用户名密码注册：委托给策略，返回RegisterResponse且字段正确")
                void whenUsernamePasswordRegister_thenDelegateAndReturnResponse() {
                        // given
                        UsernamePasswordRegisterRequest request = new UsernamePasswordRegisterRequest(
                                        RegisterType.USERNAME_PASSWORD,
                                        USERNAME,
                                        PASSWORD,
                                        USER_DOMAIN);

                        AuthenticationResult authResult = authResult();
                        RegisterStrategy mockStrategy = mock(RegisterStrategy.class);
                        doReturn(mockStrategy).when(registerStrategyFactory)
                                        .getStrategy(RegisterType.USERNAME_PASSWORD);
                        when(mockStrategy.execute(any(RegistrationContext.class))).thenReturn(authResult);

                        // when
                        RegisterResponse response = service.register(request, "127.0.0.1", "Mozilla/5.0");

                        // then
                        assertThat(response).isNotNull();
                        assertThat(response.userId()).isEqualTo(USER_ID.value());
                        assertThat(response.userDomain()).isEqualTo(USER_DOMAIN.name());
                        assertThat(response.nickname()).isEqualTo(USERNAME);
                        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
                        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
                        assertThat(response.message()).isEqualTo("注册成功");

                        verify(registerStrategyFactory).getStrategy(RegisterType.USERNAME_PASSWORD);
                        verify(registrationValidationChain).validate(any(RegistrationContext.class));
                        verify(mockStrategy).execute(any(RegistrationContext.class));
                }

                @Test
                @DisplayName("userAgent为null时，使用默认值'Unknown'，不抛出NPE")
                void whenUserAgentNull_thenUseDefaultValue() {
                        // given
                        UsernamePasswordRegisterRequest request = new UsernamePasswordRegisterRequest(
                                        RegisterType.USERNAME_PASSWORD,
                                        USERNAME,
                                        PASSWORD,
                                        USER_DOMAIN);

                        RegisterStrategy mockStrategy = mock(RegisterStrategy.class);
                        doReturn(mockStrategy).when(registerStrategyFactory).getStrategy(any());
                        when(mockStrategy.execute(any())).thenReturn(authResult());

                        service.register(request, "127.0.0.1", null);
                        verify(mockStrategy).execute(any());
                }

                @Test
                @DisplayName("策略抛出DomainException，异常向上传播")
                void whenStrategyThrows_thenPropagateException() {
                        // given
                        UsernamePasswordRegisterRequest request = new UsernamePasswordRegisterRequest(
                                        RegisterType.USERNAME_PASSWORD,
                                        USERNAME,
                                        PASSWORD,
                                        USER_DOMAIN);

                        RegisterStrategy mockStrategy = mock(RegisterStrategy.class);
                        doReturn(mockStrategy).when(registerStrategyFactory).getStrategy(any());
                        when(mockStrategy.execute(any()))
                                        .thenThrow(new DomainException(AuthResultCode.USERNAME_ALREADY_EXISTS));

                        // when & then
                        assertThatThrownBy(() -> service.register(request, "127.0.0.1", "UA"))
                                        .isInstanceOf(DomainException.class);
                }
        }

        @Nested
        @DisplayName("OAuth2 注册（委托给 OneStopAuthenticationService）")
        class OAuth2Register {

                @Test
                @DisplayName("OAuth2注册：委托给OneStopAuth，将响应转换为RegisterResponse")
                void whenOAuth2Register_thenDelegateToOneStopAuth() {
                        // given
                        OAuth2RegisterRequest request = new OAuth2RegisterRequest(
                                        RegisterType.OAUTH2,
                                        OAuth2Provider.GOOGLE,
                                        "auth_code_123",
                                        "state_xyz",
                                        USER_DOMAIN);

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

                        // when
                        RegisterResponse response = service.register(request, "127.0.0.1", "UA");

                        // then
                        assertThat(response).isNotNull();
                        assertThat(response.userId()).isEqualTo(USER_ID.value());
                        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);

                        verify(oneStopAuthenticationService).authenticate(any(), eq("127.0.0.1"), eq("UA"));
                        verifyNoInteractions(registerStrategyFactory, registrationValidationChain);
                }
        }

        @Nested
        @DisplayName("微信注册（委托给 OneStopAuthenticationService）")
        class WeChatRegister {

                @Test
                @DisplayName("微信注册：委托给OneStopAuth，将响应转换为RegisterResponse")
                void whenWeChatRegister_thenDelegateToOneStopAuth() {
                        // given
                        WeChatRegisterRequest request = new WeChatRegisterRequest(
                                        RegisterType.WECHAT,
                                        "wechat_code_abc",
                                        "wechat_state",
                                        USER_DOMAIN);

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

                        // when
                        RegisterResponse response = service.register(request, "10.0.0.1", "WeChat/8.0");

                        // then
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
