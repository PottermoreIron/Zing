package com.pot.auth.interfaces;

import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.application.service.RegistrationApplicationService;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.interfaces.controller.RegistrationController;
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

@WebMvcTest(controllers = RegistrationController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "pot.ratelimit.enabled=false"
})
@DisplayName("RegistrationController 切片测试")
class RegistrationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private RegistrationApplicationService registrationApplicationService;

        private static final String REGISTER_URL = "/auth/api/v1/register";

        @Nested
        @DisplayName("用户名密码注册")
        class UsernamePasswordRegister {

                @Test
                @DisplayName("合法的用户名密码注册请求，返回200和RegisterResponse")
                void whenValidRequest_thenReturn200WithResponse() throws Exception {
                        long now = System.currentTimeMillis() / 1000;
                        RegisterResponse response = RegisterResponse.success(
                                        TestFixtures.USER_ID.value(),
                                        TestFixtures.USER_DOMAIN.name(),
                                        TestFixtures.USERNAME,
                                        TestFixtures.EMAIL,
                                        TestFixtures.PHONE,
                                        TestFixtures.FAKE_ACCESS_TOKEN,
                                        TestFixtures.FAKE_REFRESH_TOKEN,
                                        now + 3600,
                                        now + 2592000);
                        when(registrationApplicationService.register(any(), any(), any()))
                                        .thenReturn(response);

                        String requestBody = """
                                        {
                                          "registerType": "USERNAME_PASSWORD",
                                                                                                            "nickname": "test_user",
                                          "password": "Password123!",
                                          "userDomain": "member"
                                        }
                                        """;

                        mockMvc.perform(post(REGISTER_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.accessToken").value(TestFixtures.FAKE_ACCESS_TOKEN))
                                        .andExpect(jsonPath("$.data.refreshToken")
                                                        .value(TestFixtures.FAKE_REFRESH_TOKEN))
                                        .andExpect(jsonPath("$.data.userId").value(TestFixtures.USER_ID.value()))
                                        .andExpect(jsonPath("$.data.message").value("注册成功"));
                }

                @Test
                @DisplayName("密码格式不合法（太短），返回400")
                void whenPasswordTooShort_thenReturn400() throws Exception {
                        String requestBody = """
                                        {
                                          "registerType": "USERNAME_PASSWORD",
                                                                                                            "nickname": "test_user",
                                          "password": "weak",
                                          "userDomain": "member"
                                        }
                                        """;

                        mockMvc.perform(post(REGISTER_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("未知的 registerType，反序列化失败返回400")
                void whenUnknownRegisterType_thenReturn400() throws Exception {
                        String requestBody = """
                                        {
                                          "registerType": "UNKNOWN_TYPE",
                                                                                                            "nickname": "test_user",
                                          "password": "Password123!",
                                          "userDomain": "member"
                                        }
                                        """;

                        mockMvc.perform(post(REGISTER_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("服务抛出 USERNAME_ALREADY_EXISTS，返回400并携带错误码")
                void whenUsernameAlreadyExists_thenReturn400WithErrorCode() throws Exception {
                        when(registrationApplicationService.register(any(), any(), any()))
                                        .thenThrow(new DomainException(AuthResultCode.USERNAME_ALREADY_EXISTS));

                        String requestBody = """
                                        {
                                          "registerType": "USERNAME_PASSWORD",
                                                                                                            "nickname": "test_user",
                                          "password": "Password123!",
                                          "userDomain": "member"
                                        }
                                        """;

                        mockMvc.perform(post(REGISTER_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.success").value(false))
                                        .andExpect(jsonPath("$.code").value("AUTH_0300"));
                }
        }

        @Nested
        @DisplayName("邮箱验证码注册")
        class EmailCodeRegister {

                @Test
                @DisplayName("合法的邮箱验证码注册请求，返回200")
                void whenValidEmailCodeRequest_thenReturn200() throws Exception {
                        long now = System.currentTimeMillis() / 1000;
                        when(registrationApplicationService.register(any(), any(), any()))
                                        .thenReturn(RegisterResponse.success(
                                                        TestFixtures.USER_ID.value(),
                                                        TestFixtures.USER_DOMAIN.name(),
                                                        TestFixtures.USERNAME,
                                                        TestFixtures.EMAIL,
                                                        null,
                                                        TestFixtures.FAKE_ACCESS_TOKEN,
                                                        TestFixtures.FAKE_REFRESH_TOKEN,
                                                        now + 3600,
                                                        now + 2592000));

                        String requestBody = """
                                        {
                                          "registerType": "EMAIL_CODE",
                                          "email": "test@example.com",
                                          "code": "123456",
                                          "userDomain": "member"
                                        }
                                        """;

                        mockMvc.perform(post(REGISTER_URL)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true));
                }
        }
}
