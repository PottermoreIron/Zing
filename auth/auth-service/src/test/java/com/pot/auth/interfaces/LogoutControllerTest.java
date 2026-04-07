package com.pot.auth.interfaces;

import com.pot.auth.application.service.LogoutApplicationService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.interfaces.controller.LogoutController;
import com.pot.auth.interfaces.exception.GlobalExceptionHandler;
import com.pot.auth.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
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
@DisplayName("LogoutController 切片测试")
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
                @DisplayName("提供合法Bearer Token，返回200成功")
                void whenBearerTokenProvided_thenReturn200() throws Exception {
                        // Logout is idempotent, so no extra mock behavior is required.
                        doNothing().when(logoutApplicationService).logout(anyString(), isNull());

                        mockMvc.perform(post(LOGOUT_URL)
                                        .header("Authorization", "Bearer " + TestFixtures.FAKE_ACCESS_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true));

                        // The controller should pass the raw token without the Bearer prefix.
                        verify(logoutApplicationService).logout(TestFixtures.FAKE_ACCESS_TOKEN, null);
                }

                @Test
                @DisplayName("缺少Authorization请求头，返回400并携带TOKEN_INVALID错误码")
                void whenAuthorizationHeaderMissing_thenReturn400WithTokenInvalidCode() throws Exception {
                        mockMvc.perform(post(LOGOUT_URL)
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(status().isOk()) // R.fail() keeps HTTP 200 and reports failure in
                                                                    // the body.
                                        .andExpect(jsonPath("$.success").value(false))
                                        .andExpect(jsonPath("$.code").value(AuthResultCode.TOKEN_INVALID.getCode()));
                }

                @Test
                @DisplayName("同时提供refreshToken，两者均传递给服务层")
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
                @DisplayName("Authorization头无Bearer前缀（裸token），正常解析")
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
