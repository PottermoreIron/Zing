package com.pot.auth.application;

import com.pot.auth.application.service.LogoutApplicationService;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutApplicationService 单元测试")
class LogoutApplicationServiceTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private LogoutApplicationService logoutApplicationService;

    @Test
    @DisplayName("提供AccessToken和RefreshToken，两者均委托给JwtTokenService")
    void whenBothTokensProvided_thenDelegateToBothRevocations() {
        logoutApplicationService.logout(TestFixtures.FAKE_ACCESS_TOKEN, TestFixtures.FAKE_REFRESH_TOKEN);

        verify(jwtTokenService).logout(TestFixtures.FAKE_ACCESS_TOKEN, TestFixtures.FAKE_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("仅提供AccessToken（RefreshToken为null），委托时RefreshToken为null")
    void whenOnlyAccessToken_thenDelegateWithNullRefresh() {
        logoutApplicationService.logout(TestFixtures.FAKE_ACCESS_TOKEN, null);

        verify(jwtTokenService).logout(TestFixtures.FAKE_ACCESS_TOKEN, null);
    }
}
