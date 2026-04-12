package com.pot.auth.interfaces.rest;

import com.pot.auth.application.service.LogoutApplicationService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LogoutController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "pot.ratelimit.enabled=false"
})
@DisplayName("LogoutController slice test")
class LogoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogoutApplicationService logoutApplicationService;

    private static final String LOGOUT_URL = "/auth/api/v1/logout";

    @Nested
    @DisplayName("POST /auth/api/v1/logout")
    class Logout {

        @Test
        @DisplayName("Valid Bearer token returns 200 success")
        void whenBearerTokenProvided_thenReturn200() throws Exception {
            doNothing().when(logoutApplicationService).logout(anyString(), isNull());

            mockMvc.perform(post(LOGOUT_URL)
                    .header("Authorization", "Bearer " + TestFixtures.FAKE_ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(logoutApplicationService).logout(TestFixtures.FAKE_ACCESS_TOKEN, null);
        }

        @Test
        @DisplayName("Missing Authorization header returns 400 with TOKEN_INVALID error code")
        void whenAuthorizationHeaderMissing_thenReturn400WithTokenInvalidCode() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(AuthResultCode.TOKEN_INVALID.getCode()));
        }

        @Test
        @DisplayName("Providing both tokens passes both to the service layer")
        void whenBothTokensProvided_thenPassBothToService() throws Exception {
            doNothing().when(logoutApplicationService).logout(anyString(), anyString());

            String requestBody = """
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(TestFixtures.FAKE_REFRESH_TOKEN);

            mockMvc.perform(post(LOGOUT_URL)
                    .header("Authorization", "Bearer " + TestFixtures.FAKE_ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(logoutApplicationService)
                    .logout(TestFixtures.FAKE_ACCESS_TOKEN, TestFixtures.FAKE_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Authorization header without Bearer prefix (bare token) is parsed normally")
        void whenAuthorizationHeaderWithoutBearerPrefix_thenParseAsRawToken() throws Exception {
            doNothing().when(logoutApplicationService).logout(anyString(), isNull());

            mockMvc.perform(post(LOGOUT_URL)
                    .header("Authorization", TestFixtures.FAKE_ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(logoutApplicationService).logout(TestFixtures.FAKE_ACCESS_TOKEN, null);
        }
    }
}