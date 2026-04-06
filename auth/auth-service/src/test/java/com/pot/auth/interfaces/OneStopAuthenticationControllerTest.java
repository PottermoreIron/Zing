package com.pot.auth.interfaces;

import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.service.OneStopAuthenticationService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.interfaces.controller.OneStopAuthenticationController;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OneStopAuthenticationController 切片测试（@WebMvcTest）
 *
 * <p>
 * 仅加载Web层Bean，所有应用服务均使用MockBean替代。
 *
 * @author pot
 */
@WebMvcTest(controllers = OneStopAuthenticationController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "pot.ratelimit.enabled=false"
})
@DisplayName("OneStopAuthenticationController 切片测试")
class OneStopAuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OneStopAuthenticationService oneStopAuthenticationService;

    private static final String AUTH_URL = "/auth/api/v1/authenticate";

    // ================================================================
    // 用户名密码认证
    // ================================================================

    @Nested
    @DisplayName("用户名密码一键认证")
    class UsernamePasswordAuth {

        @Test
        @DisplayName("合法的用户名密码请求，返回200和OneStopAuthResponse")
        void whenValidRequest_thenReturn200WithResponse() throws Exception {
            // given
            long now = System.currentTimeMillis() / 1000;
            OneStopAuthResponse authResponse = OneStopAuthResponse.builder()
                    .userId(TestFixtures.USER_ID)
                    .userDomain(TestFixtures.USER_DOMAIN)
                    .nickname(TestFixtures.USERNAME)
                    .email(TestFixtures.EMAIL)
                    .phone(TestFixtures.PHONE)
                    .accessToken(TestFixtures.FAKE_ACCESS_TOKEN)
                    .refreshToken(TestFixtures.FAKE_REFRESH_TOKEN)
                    .accessTokenExpiresAt(now + 3600)
                    .refreshTokenExpiresAt(now + 2592000)
                    .build();
            when(oneStopAuthenticationService.authenticate(any(), any(), any()))
                    .thenReturn(authResponse);

            String requestBody = """
                    {
                      "authType": "USERNAME_PASSWORD",
                                                                                        "nickname": "test_user",
                      "password": "Password123!",
                      "userDomain": "member"
                    }
                    """;

            // when & then
            mockMvc.perform(post(AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value(TestFixtures.FAKE_ACCESS_TOKEN))
                    .andExpect(jsonPath("$.data.refreshToken").value(TestFixtures.FAKE_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("密码格式不合法，返回400")
        void whenInvalidPassword_thenReturn400() throws Exception {
            String requestBody = """
                    {
                      "authType": "USERNAME_PASSWORD",
                                                                                        "nickname": "test_user",
                      "password": "weak",
                      "userDomain": "member"
                    }
                    """;

            mockMvc.perform(post(AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("未知的 authType，反序列化失败返回400")
        void whenUnknownAuthType_thenReturn400() throws Exception {
            String requestBody = """
                    {
                      "authType": "UNKNOWN_TYPE",
                                                                                        "nickname": "test_user",
                      "password": "Password123!",
                      "userDomain": "member"
                    }
                    """;

            mockMvc.perform(post(AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("服务抛出 AUTHENTICATION_FAILED，返回400并携带 AUTH_0001 错误码")
        void whenAuthFailed_thenReturn400WithErrorCode() throws Exception {
            // given
            when(oneStopAuthenticationService.authenticate(any(), any(), any()))
                    .thenThrow(new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

            String requestBody = """
                    {
                      "authType": "USERNAME_PASSWORD",
                                                                                        "nickname": "test_user",
                      "password": "Password123!",
                      "userDomain": "member"
                    }
                    """;

            // when & then
            mockMvc.perform(post(AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH_0001"));
        }
    }

    // ================================================================
    // 手机验证码认证
    // ================================================================

    @Nested
    @DisplayName("手机验证码一键认证")
    class PhoneCodeAuth {

        @Test
        @DisplayName("合法手机验证码请求，返回200")
        void whenValidPhoneCodeRequest_thenReturn200() throws Exception {
            // given
            long now = System.currentTimeMillis() / 1000;
            when(oneStopAuthenticationService.authenticate(any(), any(), any()))
                    .thenReturn(OneStopAuthResponse.builder()
                            .userId(TestFixtures.USER_ID)
                            .userDomain(TestFixtures.USER_DOMAIN)
                            .nickname(TestFixtures.USERNAME)
                            .accessToken(TestFixtures.FAKE_ACCESS_TOKEN)
                            .refreshToken(TestFixtures.FAKE_REFRESH_TOKEN)
                            .accessTokenExpiresAt(now + 3600)
                            .refreshTokenExpiresAt(now + 2592000)
                            .build());

            String requestBody = """
                    {
                      "authType": "PHONE_CODE",
                      "phone": "13800138000",
                      "code": "123456",
                      "userDomain": "member"
                    }
                    """;

            mockMvc.perform(post(AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ================================================================
    // 邮箱验证码认证
    // ================================================================

    @Nested
    @DisplayName("邮箱验证码一键认证")
    class EmailCodeAuth {

        @Test
        @DisplayName("合法邮箱验证码请求，返回200")
        void whenValidEmailCodeRequest_thenReturn200() throws Exception {
            // given
            long now = System.currentTimeMillis() / 1000;
            when(oneStopAuthenticationService.authenticate(any(), any(), any()))
                    .thenReturn(OneStopAuthResponse.builder()
                            .userId(TestFixtures.USER_ID)
                            .userDomain(TestFixtures.USER_DOMAIN)
                            .nickname(TestFixtures.USERNAME)
                            .accessToken(TestFixtures.FAKE_ACCESS_TOKEN)
                            .refreshToken(TestFixtures.FAKE_REFRESH_TOKEN)
                            .accessTokenExpiresAt(now + 3600)
                            .refreshTokenExpiresAt(now + 2592000)
                            .build());

            String requestBody = """
                    {
                      "authType": "EMAIL_CODE",
                      "email": "test@example.com",
                      "code": "123456",
                      "userDomain": "member"
                    }
                    """;

            mockMvc.perform(post(AUTH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
