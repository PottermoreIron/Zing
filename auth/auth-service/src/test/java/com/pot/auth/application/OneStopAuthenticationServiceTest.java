package com.pot.auth.application;

import com.pot.auth.application.command.OneStopAuthCommand;
import com.pot.auth.application.command.OneStopAuthRequestCommand;
import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.service.OneStopAuthenticationService;
import com.pot.auth.application.strategy.OneStopAuthStrategy;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.strategy.factory.OneStopAuthStrategyFactory;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OneStopAuthenticationService unit test")
class OneStopAuthenticationServiceTest {

    private static final UserId USER_ID = UserId.of(10001L);
    private static final UserDomain USER_DOMAIN = UserDomain.MEMBER;
    private static final String USERNAME = "test_user";
    private static final String PASSWORD = "Password123!";
    private static final String EMAIL = "test@example.com";
    private static final String PHONE = "+8613800138000";
    private static final String ACCESS_TOKEN = "fake.access.token";
    private static final String REFRESH_TOKEN = "fake.refresh.token";

    @Mock
    private OneStopAuthStrategyFactory strategyFactory;

    @Mock
    private ValidationChain<OneStopAuthContext> oneStopAuthValidationChain;

    @InjectMocks
    private OneStopAuthenticationService service;

    @Test
    @DisplayName("Username-password authentication: context built correctly, delegates to strategy, returns OneStopAuthResponse")
    void whenUsernamePasswordAuth_thenBuildContextAndReturnResponse() {
        OneStopAuthCommand command = new OneStopAuthRequestCommand(
                AuthType.USERNAME_PASSWORD,
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
        OneStopAuthStrategy mockStrategy = mock(OneStopAuthStrategy.class);
        doReturn(mockStrategy).when(strategyFactory).getStrategy(AuthType.USERNAME_PASSWORD);
        when(mockStrategy.execute(any(OneStopAuthContext.class))).thenReturn(authResult);

        OneStopAuthResponse response = service.authenticate(command, "192.168.1.1", "Chrome/120.0");

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.userDomain()).isEqualTo(USER_DOMAIN);
        assertThat(response.nickname()).isEqualTo(USERNAME);
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);

        verify(strategyFactory).getStrategy(AuthType.USERNAME_PASSWORD);

        ArgumentCaptor<OneStopAuthContext> contextCaptor = ArgumentCaptor.forClass(OneStopAuthContext.class);
        verify(mockStrategy).execute(contextCaptor.capture());
        OneStopAuthContext capturedCtx = contextCaptor.getValue();
        assertThat(capturedCtx.request().authType()).isEqualTo(command.authType());
        assertThat(capturedCtx.request().userDomain()).isEqualTo(command.userDomain());
        assertThat(capturedCtx.request().nickname()).isEqualTo(command.nickname());
        assertThat(capturedCtx.request().password()).isEqualTo(command.password());
        assertThat(capturedCtx.ipAddress().value()).isEqualTo("192.168.1.1");
        verify(oneStopAuthValidationChain).validate(any(OneStopAuthContext.class));
    }

    @Test
    @DisplayName("Null userAgent uses default value 'Unknown' without throwing NPE")
    void whenUserAgentNull_thenUseDefaultAndNotThrow() {
        OneStopAuthCommand command = new OneStopAuthRequestCommand(
                AuthType.USERNAME_PASSWORD,
                USER_DOMAIN,
                USERNAME,
                null,
                null,
                PASSWORD,
                null,
                null,
                null,
                null);

        OneStopAuthStrategy mockStrategy = mock(OneStopAuthStrategy.class);
        doReturn(mockStrategy).when(strategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenReturn(authResult());

        OneStopAuthResponse response = service.authenticate(command, "127.0.0.1", null);
        assertThat(response).isNotNull();
        verify(mockStrategy).execute(any());
    }

    @Test
    @DisplayName("Strategy throws DomainException which propagates upward")
    void whenStrategyThrowsDomainException_thenPropagateException() {
        OneStopAuthCommand command = new OneStopAuthRequestCommand(
                AuthType.USERNAME_PASSWORD,
                USER_DOMAIN,
                USERNAME,
                null,
                null,
                PASSWORD,
                null,
                null,
                null,
                null);

        OneStopAuthStrategy mockStrategy = mock(OneStopAuthStrategy.class);
        doReturn(mockStrategy).when(strategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenThrow(new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        assertThatThrownBy(() -> service.authenticate(command, "127.0.0.1", "UA"))
                .isInstanceOf(DomainException.class);
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
