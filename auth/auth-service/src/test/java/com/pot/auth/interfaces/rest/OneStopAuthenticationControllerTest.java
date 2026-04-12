package com.pot.auth.interfaces.rest;

import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.service.OneStopAuthenticationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OneStopAuthenticationController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "pot.ratelimit.enabled=false"
})
@DisplayName("OneStopAuthenticationController slice test")
class OneStopAuthenticationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private OneStopAuthenticationService oneStopAuthenticationService;

        @MockitoBean
        private AuthCommandAssembler authCommandAssembler;

        private static final String AUTH_URL = "/auth/api/v1/authenticate";

        @Nested
        @DisplayName("One-stop authentication with username and password")
        class UsernamePasswordAuth {

                @Test
                @DisplayName("Valid username-password request returns 200 with OneStopAuthResponse")
                void whenValidRequest_thenReturn200WithResponse() throws Exception {
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

                        mockMvc.perform(post(AUTH_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.accessToken").value(TestFixtures.FAKE_ACCESS_TOKEN))
                                        .andExpect(jsonPath("$.data.refreshToken")
                                                        .value(TestFixtures.FAKE_REFRESH_TOKEN));
                }

                @Test
                @DisplayName("Invalid password format returns 400")
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
                @DisplayName("Unknown authType fails deserialization and returns 400")
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
                @DisplayName("Service throws AUTHENTICATION_FAILED returns 400 with AUTH_0001 error code")
                void whenAuthFailed_thenReturn400WithErrorCode() throws Exception {
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

                        mockMvc.perform(post(AUTH_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.success").value(false))
                                        .andExpect(jsonPath("$.code").value("AUTH_0001"));
                }
        }

        @Nested
        @DisplayName("One-stop authentication with phone verification code")
        class PhoneCodeAuth {

                @Test
                @DisplayName("Valid phone verification code request returns 200")
                void whenValidPhoneCodeRequest_thenReturn200() throws Exception {
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

        @Nested
        @DisplayName("One-stop authentication with email verification code")
        class EmailCodeAuth {

                @Test
                @DisplayName("Valid email verification code request returns 200")
                void whenValidEmailCodeRequest_thenReturn200() throws Exception {
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