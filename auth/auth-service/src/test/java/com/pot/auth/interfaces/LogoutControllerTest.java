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

/**
 * LogoutController 切片测试（@WebMvcTest）
 *
 * <p>
 * 验证登出接口的HTTP协议行为：
 * <ul>
 * <li>Authorization头存在时，成功登出</li>
 * <li>Authorization头缺失时，返回TOKEN_INVALID错误</li>
 * <li>同时提供refreshToken时，两者均传递到服务层</li>
 * </ul>
 *
 * @author pot
 */
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

    // ================================================================
    // POST /auth/api/v1/logout
    // ================================================================

    @Nested
    @DisplayName("POST /auth/api/v1/logout")
    class Logout {

        @Test
        @DisplayName("提供合法Bearer Token，返回200成功")
        void whenBearerTokenProvided_thenReturn200() throws Exception {
            // given: LogoutApplicationService为幂等操作，不需要配置mock行为
            doNothing().when(logoutApplicationService).logout(anyString(), isNull());

            // when & then
            mockMvc.perform(post(LOGOUT_URL)
                    .header("Authorization", "Bearer " + TestFixtures.FAKE_ACCESS_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 验证accessToken正确传递（Bearer前缀已剥离）
            verify(logoutApplicationService).logout(TestFixtures.FAKE_ACCESS_TOKEN, null);
        }

        @Test
        @DisplayName("缺少Authorization请求头，返回400并携带TOKEN_INVALID错误码")
        void whenAuthorizationHeaderMissing_thenReturn400WithTokenInvalidCode() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()) // R.fail() 不是HTTP 4xx，控制器返回200但success=false
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(AuthResultCode.TOKEN_INVALID.getCode()));
        }

        @Test
        @DisplayName("同时提供refreshToken，两者均传递给服务层")
        void whenBothTokensProvided_thenPassBothToService() throws Exception {
            // given
            doNothing().when(logoutApplicationService).logout(anyString(), anyString());

            String requestBody = """
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(TestFixtures.FAKE_REFRESH_TOKEN);

            // when & then
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
