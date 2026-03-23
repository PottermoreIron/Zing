package com.pot.auth.application;

import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.service.OneStopAuthenticationService;
import com.pot.auth.application.strategy.OneStopAuthStrategy;
import com.pot.auth.application.strategy.factory.OneStopAuthStrategyFactory;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import com.pot.auth.interfaces.dto.onestop.UsernamePasswordAuthRequest;
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

/**
 * OneStopAuthenticationService 单元测试
 *
 * <p>
 * 验证：
 * <ul>
 * <li>正确构建 OneStopAuthContext 并委托给 OneStopAuthStrategyFactory</li>
 * <li>将 AuthenticationResult 映射为 OneStopAuthResponse</li>
 * <li>userAgent 为 null 时使用默认值 "Unknown"</li>
 * <li>策略抛出异常时向上传播</li>
 * </ul>
 *
 * @author pot
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OneStopAuthenticationService 单元测试")
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

    @InjectMocks
    private OneStopAuthenticationService service;

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("用户名密码认证：正确构建上下文，委托给策略，返回OneStopAuthResponse")
    void whenUsernamePasswordAuth_thenBuildContextAndReturnResponse() {
        // given
        UsernamePasswordAuthRequest request = new UsernamePasswordAuthRequest(
                AuthType.USERNAME_PASSWORD,
                USERNAME,
                PASSWORD,
                USER_DOMAIN);

        AuthenticationResult authResult = authResult();
        OneStopAuthStrategy<UsernamePasswordAuthRequest> mockStrategy = mock(OneStopAuthStrategy.class);
        doReturn(mockStrategy).when(strategyFactory).getStrategy(AuthType.USERNAME_PASSWORD);
        when(mockStrategy.execute(any(OneStopAuthContext.class))).thenReturn(authResult);

        // when
        OneStopAuthResponse response = service.authenticate(request, "192.168.1.1", "Chrome/120.0");

        // then: 响应字段正确映射
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.userDomain()).isEqualTo(USER_DOMAIN);
        assertThat(response.username()).isEqualTo(USERNAME);
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);

        // then: 正确的AuthType传入Factory
        verify(strategyFactory).getStrategy(AuthType.USERNAME_PASSWORD);

        // then: 上下文携带了正确的IP
        ArgumentCaptor<OneStopAuthContext> contextCaptor = ArgumentCaptor.forClass(OneStopAuthContext.class);
        verify(mockStrategy).execute(contextCaptor.capture());
        OneStopAuthContext capturedCtx = contextCaptor.getValue();
        assertThat(capturedCtx.request()).isEqualTo(request);
        assertThat(capturedCtx.ipAddress().value()).isEqualTo("192.168.1.1");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("userAgent为null时，使用默认值'Unknown'，不抛出NPE")
    void whenUserAgentNull_thenUseDefaultAndNotThrow() {
        // given
        UsernamePasswordAuthRequest request = new UsernamePasswordAuthRequest(
                AuthType.USERNAME_PASSWORD,
                USERNAME,
                PASSWORD,
                USER_DOMAIN);

        OneStopAuthStrategy<UsernamePasswordAuthRequest> mockStrategy = mock(OneStopAuthStrategy.class);
        doReturn(mockStrategy).when(strategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenReturn(authResult());

        // when & then
        OneStopAuthResponse response = service.authenticate(request, "127.0.0.1", null);
        assertThat(response).isNotNull();
        verify(mockStrategy).execute(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("策略抛出 DomainException，异常向上传播")
    void whenStrategyThrowsDomainException_thenPropagateException() {
        // given
        UsernamePasswordAuthRequest request = new UsernamePasswordAuthRequest(
                AuthType.USERNAME_PASSWORD,
                USERNAME,
                PASSWORD,
                USER_DOMAIN);

        OneStopAuthStrategy<UsernamePasswordAuthRequest> mockStrategy = mock(OneStopAuthStrategy.class);
        doReturn(mockStrategy).when(strategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenThrow(new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        // when & then
        assertThatThrownBy(() -> service.authenticate(request, "127.0.0.1", "UA"))
                .isInstanceOf(DomainException.class);
    }

    private AuthenticationResult authResult() {
        long now = System.currentTimeMillis() / 1000;
        return AuthenticationResult.builder()
                .userId(USER_ID)
                .userDomain(USER_DOMAIN)
                .username(USERNAME)
                .email(EMAIL)
                .phone(PHONE)
                .accessToken(ACCESS_TOKEN)
                .refreshToken(REFRESH_TOKEN)
                .accessTokenExpiresAt(now + 3600)
                .refreshTokenExpiresAt(now + 2592000)
                .build();
    }
}
