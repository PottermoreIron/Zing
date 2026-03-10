package com.pot.auth.interfaces;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.LoginApplicationService;
import com.pot.auth.application.service.TokenRefreshApplicationService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.interfaces.controller.LoginController;
import com.pot.auth.interfaces.exception.GlobalExceptionHandler;
import com.pot.auth.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LoginController 切片测试（@WebMvcTest）
 *
 * <p>
 * 仅加载Web层Bean，所有应用服务均使用MockBean替代，
 * 专注验证：HTTP协议、请求反序列化、校验逻辑、异常处理。
 *
 * @author pot
 */
@WebMvcTest(controllers = LoginController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "pot.ratelimit.enabled=false"
})
@DisplayName("LoginController 切片测试")
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginApplicationService loginApplicationService;

    @MockitoBean
    private TokenRefreshApplicationService tokenRefreshApplicationService;

    // ================================================================
    // POST /auth/api/v1/login - 登录
    // ================================================================

    @Nested
    @DisplayName("POST /auth/api/v1/login")
    class Login {

        private static final String LOGIN_URL = "/auth/api/v1/login";

        @Test
        @DisplayName("用户名密码登录请求合法，返回200和LoginResponse")
        void whenValidUsernamePasswordRequest_thenReturn200WithLoginResponse() throws Exception {
            // given
            LoginResponse loginResponse = TestFixtures.loginResponse();
            when(loginApplicationService.login(any(), any(), any()))
                    .thenReturn(loginResponse);

            String requestBody = """
                    {
                      "loginType": "USERNAME_PASSWORD",
                      "username": "test_user",
                      "password": "Password123!",
                      "userDomain": "member"
                    }
                    """;

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value(TestFixtures.FAKE_ACCESS_TOKEN))
                    .andExpect(jsonPath("$.data.refreshToken").value(TestFixtures.FAKE_REFRESH_TOKEN))
                    .andExpect(jsonPath("$.data.userId").value(TestFixtures.USER_ID.value()))
                    .andExpect(jsonPath("$.data.username").value(TestFixtures.USERNAME));
        }

        @Test
        @DisplayName("未知loginType，Jackson反序列化失败返回400")
        void whenUsernameMissing_thenReturn400ValidationError() throws Exception {
            // given: 未知的loginType触发反序列化失败 -> HttpMessageNotReadableException -> 400
            // 注：@ValidUsername允许null（defer to domain层），缺少username不会触发constraint violation
            String requestBody = """
                    {
                      "loginType": "UNKNOWN_TYPE",
                      "password": "Password123!",
                      "userDomain": "member"
                    }
                    """;

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("密码格式不合法（太短），返回400")
        void whenPasswordTooShort_thenReturn400() throws Exception {
            String requestBody = """
                    {
                      "loginType": "USERNAME_PASSWORD",
                      "username": "test_user",
                      "password": "weak",
                      "userDomain": "member"
                    }
                    """;

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("服务抛出AUTHENTICATION_FAILED，返回400并携带AUTH_0001错误码")
        void whenServiceThrowsAuthFailed_thenReturn400WithAuthErrorCode() throws Exception {
            // given: 认证失败
            when(loginApplicationService.login(any(), any(), any()))
                    .thenThrow(new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

            String requestBody = """
                    {
                      "loginType": "USERNAME_PASSWORD",
                      "username": "test_user",
                      "password": "Password123!",
                      "userDomain": "member"
                    }
                    """;

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH_0001"));
        }

        @Test
        @DisplayName("邮箱验证码登录请求合法，返回200")
        void whenValidEmailCodeRequest_thenReturn200() throws Exception {
            // given
            when(loginApplicationService.login(any(), any(), any()))
                    .thenReturn(TestFixtures.loginResponse());

            String requestBody = """
                    {
                      "loginType": "EMAIL_CODE",
                      "email": "test@example.com",
                      "code": "123456",
                      "userDomain": "member"
                    }
                    """;

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ================================================================
    // POST /auth/api/v1/refresh - 刷新Token
    // ================================================================

    @Nested
    @DisplayName("POST /auth/api/v1/refresh")
    class RefreshToken {

        private static final String REFRESH_URL = "/auth/api/v1/refresh";

        @Test
        @DisplayName("有效refreshToken，返回200和新的TokenPair")
        void whenValidRefreshToken_thenReturn200() throws Exception {
            // given
            when(tokenRefreshApplicationService.refreshToken(anyString()))
                    .thenReturn(TestFixtures.loginResponse());

            String requestBody = """
                    {
                      "refreshToken": "valid.refresh.token"
                    }
                    """;

            mockMvc.perform(post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value(TestFixtures.FAKE_ACCESS_TOKEN));
        }

        @Test
        @DisplayName("refreshToken为空，返回400")
        void whenRefreshTokenBlank_thenReturn400() throws Exception {
            String requestBody = """
                    {
                      "refreshToken": ""
                    }
                    """;

            mockMvc.perform(post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }
}
