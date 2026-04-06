package com.pot.auth.application;

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
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import com.pot.auth.interfaces.dto.auth.UsernamePasswordLoginRequest;
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

/**
 * Unit tests for LoginApplicationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginApplicationService 单元测试")
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
    @DisplayName("用户名密码登录：委托给策略，返回正确的LoginResponse")
    void whenUsernamePasswordLogin_thenDelegateToStrategyAndReturnResponse() {
        // given
        LoginRequest request = usernamePasswordRequest();
        AuthenticationResult authResult = authResult();

        LoginStrategy mockStrategy = mock(LoginStrategy.class);
        doReturn(mockStrategy).when(loginStrategyFactory).getStrategy(LoginType.USERNAME_PASSWORD);
        when(mockStrategy.execute(any(AuthenticationContext.class))).thenReturn(authResult);

        // when
        LoginResponse response = loginApplicationService.login(request, "127.0.0.1", "Mozilla/5.0");

        assertThat(response.userId()).isEqualTo(USER_ID.value());
        assertThat(response.userDomain()).isEqualTo(USER_DOMAIN.name());
        assertThat(response.nickname()).isEqualTo(USERNAME);
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);

        ArgumentCaptor<AuthenticationContext> contextCaptor = ArgumentCaptor.forClass(AuthenticationContext.class);
        verify(mockStrategy).execute(contextCaptor.capture());
        AuthenticationContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.request().loginType()).isEqualTo(request.loginType());
        assertThat(capturedContext.request().userDomain()).isEqualTo(request.userDomain());
        assertThat(capturedContext.request().nickname()).isEqualTo(request.nickname());
        assertThat(capturedContext.request().password()).isEqualTo(request.password());
        assertThat(capturedContext.ipAddress().value()).isEqualTo("127.0.0.1");
        assertThat(capturedContext.sessionId()).isNotBlank();
        verify(authenticationValidationChain).validate(any(AuthenticationContext.class));
    }

    @Test
    @DisplayName("策略抛出DomainException，异常向上传播")
    void whenStrategyThrowsDomainException_thenPropagateException() {
        // given
        LoginRequest request = usernamePasswordRequest();
        LoginStrategy mockStrategy = mock(LoginStrategy.class);
        doReturn(mockStrategy).when(loginStrategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenThrow(new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        // when & then
        assertThatThrownBy(() -> loginApplicationService.login(request, "127.0.0.1", "UA"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("userAgent为null时，使用默认值'Unknown'，不抛出异常")
    void whenUserAgentNull_thenUseDefaultValue() {
        // given
        LoginRequest request = usernamePasswordRequest();
        LoginStrategy mockStrategy = mock(LoginStrategy.class);
        doReturn(mockStrategy).when(loginStrategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenReturn(authResult());

        loginApplicationService.login(request, "127.0.0.1", null);
        verify(mockStrategy).execute(any());
    }

    private LoginRequest usernamePasswordRequest() {
        return new UsernamePasswordLoginRequest(
                LoginType.USERNAME_PASSWORD,
                USERNAME,
                PASSWORD,
                USER_DOMAIN);
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
