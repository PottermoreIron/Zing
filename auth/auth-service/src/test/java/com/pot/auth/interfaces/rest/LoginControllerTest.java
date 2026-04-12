package com.pot.auth.interfaces.rest;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.service.LoginApplicationService;
import com.pot.auth.application.service.TokenRefreshApplicationService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.interfaces.assembler.AuthCommandAssembler;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoginController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "pot.ratelimit.enabled=false"
})
@DisplayName("LoginController slice test")
class LoginControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private LoginApplicationService loginApplicationService;

        @MockitoBean
        private TokenRefreshApplicationService tokenRefreshApplicationService;

        @MockitoBean
        private AuthCommandAssembler authCommandAssembler;

        @Nested
        @DisplayName("POST /auth/api/v1/login")
        class Login {

                private static final String LOGIN_URL = "/auth/api/v1/login";

                @Test
                @DisplayName("Valid username-password login request returns 200 with LoginResponse")
                void whenValidUsernamePasswordRequest_thenReturn200WithLoginResponse() throws Exception {
                        LoginResponse loginResponse = TestFixtures.loginResponse();
                        when(loginApplicationService.login(any(), any(), any()))
                                        .thenReturn(loginResponse);

                        String requestBody = """
                                        {
                                          "loginType": "USERNAME_PASSWORD",
                                                                                                            "nickname": "test_user",
                                          "password": "Password123!",
                                          "userDomain": "member"
                                        }
                                        """;

                        mockMvc.perform(post(LOGIN_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.accessToken").value(TestFixtures.FAKE_ACCESS_TOKEN))
                                        .andExpect(jsonPath("$.data.refreshToken")
                                                        .value(TestFixtures.FAKE_REFRESH_TOKEN))
                                        .andExpect(jsonPath("$.data.userId").value(TestFixtures.USER_ID.value()))
                                        .andExpect(jsonPath("$.data.nickname").value(TestFixtures.USERNAME));
                }

                @Test
                @DisplayName("Unknown loginType fails Jackson deserialization and returns 400")
                void whenUsernameMissing_thenReturn400ValidationError() throws Exception {
                        String requestBody = """
                                        {
                                          "loginType": "UNKNOWN_TYPE",
                                          "password": "Password123!",
                                          "userDomain": "member"
                                        }
                                        """;

                        mockMvc.perform(post(LOGIN_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Invalid password format (too short) returns 400")
                void whenPasswordTooShort_thenReturn400() throws Exception {
                        String requestBody = """
                                        {
                                          "loginType": "USERNAME_PASSWORD",
                                                                                                            "nickname": "test_user",
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
                @DisplayName("Service throws AUTHENTICATION_FAILED returns 400 with AUTH_0001 error code")
                void whenServiceThrowsAuthFailed_thenReturn400WithAuthErrorCode() throws Exception {
                        when(loginApplicationService.login(any(), any(), any()))
                                        .thenThrow(new DomainException(AuthResultCode.AUTHENTICATION_FAILED));

                        String requestBody = """
                                        {
                                          "loginType": "USERNAME_PASSWORD",
                                                                                                            "nickname": "test_user",
                                          "password": "Password123!",
                                          "userDomain": "member"
                                        }
                                        """;

                        mockMvc.perform(post(LOGIN_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.success").value(false))
                                        .andExpect(jsonPath("$.code").value("AUTH_0001"));
                }

                @Test
                @DisplayName("Valid email verification code login request returns 200")
                void whenValidEmailCodeRequest_thenReturn200() throws Exception {
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

        @Nested
        @DisplayName("POST /auth/api/v1/refresh")
        class RefreshToken {

                private static final String REFRESH_URL = "/auth/api/v1/refresh";

                @Test
                @DisplayName("Valid refreshToken returns 200 with new TokenPair")
                void whenValidRefreshToken_thenReturn200() throws Exception {
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
                                        .andExpect(jsonPath("$.data.accessToken")
                                                        .value(TestFixtures.FAKE_ACCESS_TOKEN));
                }

                @Test
                @DisplayName("Blank refreshToken returns 400")
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