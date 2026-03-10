package com.pot.auth.application;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.LoginApplicationService;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.context.AuthenticationContext;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.strategy.LoginStrategy;
import com.pot.auth.domain.strategy.factory.LoginStrategyFactory;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import com.pot.auth.support.TestFixtures;
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
 * LoginApplicationService 单元测试
 *
 * <p>
 * 验证应用层编排逻辑：
 * <ul>
 * <li>正确构建AuthenticationContext并委托给LoginStrategyFactory</li>
 * <li>将AuthenticationResult映射为LoginResponse</li>
 * <li>策略抛出异常时，异常向上传播</li>
 * </ul>
 *
 * @author pot
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginApplicationService 单元测试")
class LoginApplicationServiceTest {

    @Mock
    private LoginStrategyFactory loginStrategyFactory;

    @InjectMocks
    private LoginApplicationService loginApplicationService;

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("用户名密码登录：委托给策略，返回正确的LoginResponse")
    void whenUsernamePasswordLogin_thenDelegateToStrategyAndReturnResponse() {
        // given
        LoginRequest request = TestFixtures.usernamePasswordRequest();
        AuthenticationResult authResult = TestFixtures.authResult();

        LoginStrategy<LoginRequest> mockStrategy = mock(LoginStrategy.class);
        // getStrategy() 返回 LoginStrategy<?>，使用 doReturn 规避泛型类型捕获限制
        doReturn(mockStrategy).when(loginStrategyFactory).getStrategy(LoginType.USERNAME_PASSWORD);
        when(mockStrategy.execute(any(AuthenticationContext.class))).thenReturn(authResult);

        // when
        LoginResponse response = loginApplicationService.login(request, "127.0.0.1", "Mozilla/5.0");

        // then: 响应字段正确映射
        assertThat(response.userId()).isEqualTo(TestFixtures.USER_ID.value());
        assertThat(response.userDomain()).isEqualTo(TestFixtures.USER_DOMAIN.name());
        assertThat(response.username()).isEqualTo(TestFixtures.USERNAME);
        assertThat(response.accessToken()).isEqualTo(TestFixtures.FAKE_ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(TestFixtures.FAKE_REFRESH_TOKEN);

        // then: 策略调用时，AuthenticationContext正确构建
        ArgumentCaptor<AuthenticationContext> contextCaptor = ArgumentCaptor.forClass(AuthenticationContext.class);
        verify(mockStrategy).execute(contextCaptor.capture());
        AuthenticationContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.request()).isEqualTo(request);
        assertThat(capturedContext.ipAddress().value()).isEqualTo("127.0.0.1");
        assertThat(capturedContext.sessionId()).isNotBlank();
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("策略抛出DomainException，异常向上传播")
    void whenStrategyThrowsDomainException_thenPropagateException() {
        // given
        LoginRequest request = TestFixtures.usernamePasswordRequest();
        LoginStrategy<LoginRequest> mockStrategy = mock(LoginStrategy.class);
        doReturn(mockStrategy).when(loginStrategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenThrow(new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

        // when & then
        assertThatThrownBy(() -> loginApplicationService.login(request, "127.0.0.1", "UA"))
                .isInstanceOf(DomainException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("userAgent为null时，使用默认值'Unknown'，不抛出异常")
    void whenUserAgentNull_thenUseDefaultValue() {
        // given
        LoginRequest request = TestFixtures.usernamePasswordRequest();
        LoginStrategy<LoginRequest> mockStrategy = mock(LoginStrategy.class);
        doReturn(mockStrategy).when(loginStrategyFactory).getStrategy(any());
        when(mockStrategy.execute(any())).thenReturn(TestFixtures.authResult());

        // when & then: 不抛出NPE
        loginApplicationService.login(request, "127.0.0.1", null);
        verify(mockStrategy).execute(any());
    }
}
