package com.pot.auth.application;

import com.pot.auth.application.command.LoginCommand;
import com.pot.auth.application.command.LoginRequestCommand;
import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.LoginApplicationService;
import com.pot.auth.application.strategy.LoginStrategy;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.strategy.factory.LoginStrategyFactory;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
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
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginApplicationService unit test")
class LoginApplicationServiceTest {

    private static final UserId USER_ID = UserId.of(10001L);
    private static final UserDomain USER_DOMAIN = UserDomain.MEMBER;
    private static final String USERNAME = "test_user";
    private static final String PASSWORD = "Password123!";
    private static final String EMAIL = "test@example.com";
    private static final String PHONE = "+8613800138000";
    private static final String ACCESS_TOKEN = "fake.access.token";
    private static final String REFRESH_TOKEN = "fake.refresh.token";

    @Mock
    private LoginStrategyFactory loginStrategyFactory;

    @Mock
    private ValidationChain<AuthenticationContext> authenticationValidationChain;

    @InjectMocks
    private LoginApplicationService loginApplicationService;

    @Test
    @DisplayName("Username-password login: delegates to strategy and returns correct LoginResponse")
    void whenUsernamePasswordLogin_thenDelegateToStrategyAndReturnResponse() {
        LoginCommand command = usernamePasswordCommand();
        AuthenticationResult authResult = authResult();

        LoginStrategy mockStrategy = mock(LoginStrategy.class);
        doReturn(mockStrategy).when(loginStrategyFactory).getStrategy(LoginType.USERNAME_PASSWORD);
        when(mockStrategy.execute(any(AuthenticationContext.class))).thenReturn(authResult);

        LoginResponse response = loginApplicationService.login(command, "127.0.0.1", "Mozilla/5.0");

        assertThat(response.userId()).isEqualTo(USER_ID.value());
        assertThat(response.userDomain()).isEqualTo(USER_DOMAIN.name());
        assertThat(response.nickname()).isEqualTo(USERNAME);
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);

        ArgumentCaptor<AuthenticationContext> contextCaptor = ArgumentCaptor.forClass(AuthenticationContext.class);
        verify(mockStrategy).execute(contextCaptor.capture());
        AuthenticationContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.request().loginType()).isEqualTo(command.loginType());
        assertThat(capturedContext.request().userDomain()).isEqualTo(command.userDomain());
        assertThat(capturedContext.request().nickname()).isEqualTo(command.nickname());
        assertThat(capturedContext.request().password()).isEqualTo(command.password());
        assertThat(capturedContext.ipAddress().value()).isEqualTo("127.0.0.1");
        assertThat(capturedContext.sessionId()).isNotBlank();
        verify(authenticationValidationChain).validate(any(AuthenticationContext.class));
    }

    @Test
    @DisplayName("Strategy throws DomainException which propagates upward")
    void whenStrategyThrowsDomainException_thenPropagateException() {
        LoginCommand command = usernamePasswordCommand();
        LoginStrategy mockStrategy = mock(LoginStrategy.class);
        doReturn(mockStrategy).when(loginStrategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenThrow(new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        assertThatThrownBy(() -> loginApplicationService.login(command, "127.0.0.1", "UA"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Null userAgent uses default value 'Unknown' without throwing an exception")
    void whenUserAgentNull_thenUseDefaultValue() {
        LoginCommand command = usernamePasswordCommand();
        LoginStrategy mockStrategy = mock(LoginStrategy.class);
        doReturn(mockStrategy).when(loginStrategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenReturn(authResult());

        loginApplicationService.login(command, "127.0.0.1", null);
        verify(mockStrategy).execute(any());
    }

    private LoginCommand usernamePasswordCommand() {
        return new LoginRequestCommand(
                LoginType.USERNAME_PASSWORD,
                USER_DOMAIN,
                USERNAME,
                null,
                null,
                PASSWORD,
                null);
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
