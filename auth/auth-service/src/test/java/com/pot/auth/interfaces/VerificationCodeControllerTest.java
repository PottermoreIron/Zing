package com.pot.auth.interfaces;

import com.pot.auth.application.service.VerificationCodeApplicationService;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeSendTooFrequentException;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.interfaces.controller.VerificationCodeController;
import com.pot.auth.interfaces.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * VerificationCodeController 切片测试（@WebMvcTest）
 *
 * <p>
 * 验证：
 * <ul>
 * <li>合法email/phone请求返回200成功</li>
 * <li>参数格式不合法返回400</li>
 * <li>发送频率限制异常映射为AUTH_0200错误码</li>
 * <li>参数缺失返回400</li>
 * </ul>
 *
 * @author pot
 */
@WebMvcTest(controllers = VerificationCodeController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "pot.ratelimit.enabled=false"
})
@DisplayName("VerificationCodeController 切片测试")
class VerificationCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationCodeApplicationService verificationCodeApplicationService;

    // POST /auth/code/email - 发送邮件验证码

    @Nested
    @DisplayName("POST /auth/code/email")
    class SendEmailCode {

        private static final String EMAIL_CODE_URL = "/auth/code/email";

        @Test
        @DisplayName("合法邮箱地址，返回200成功")
        void whenValidEmail_thenReturn200() throws Exception {
            // given
            when(verificationCodeApplicationService.sendEmailCode(anyString())).thenReturn(true);

            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", "test@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("邮箱格式不合法，返回400")
        void whenInvalidEmailFormat_thenReturn400() throws Exception {
            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", "not-an-email"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("email参数为空，返回400")
        void whenEmailBlank_thenReturn400() throws Exception {
            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("email参数缺失，返回400")
        void whenEmailParamMissing_thenReturn400() throws Exception {
            mockMvc.perform(post(EMAIL_CODE_URL))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("发送频率限制触发，返回400并携带AUTH_0200错误码")
        void whenCodeSendTooFrequent_thenReturn400WithCode0200() throws Exception {
            // given: 触发频率限制
            when(verificationCodeApplicationService.sendEmailCode(anyString()))
                    .thenThrow(new CodeSendTooFrequentException("发送过于频繁"));

            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", "test@example.com"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(AuthResultCode.CODE_SEND_TOO_FREQUENT.getCode()));
        }

        @Test
        @DisplayName("服务返回false，接口返回失败响应")
        void whenServiceReturnsFalse_thenReturnFailResponse() throws Exception {
            when(verificationCodeApplicationService.sendEmailCode(anyString())).thenReturn(false);

            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", "test@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // POST /auth/code/sms - 发送短信验证码

    @Nested
    @DisplayName("POST /auth/code/sms")
    class SendSmsCode {

        private static final String SMS_CODE_URL = "/auth/code/sms";

        @Test
        @DisplayName("合法手机号，返回200成功")
        void whenValidPhone_thenReturn200() throws Exception {
            when(verificationCodeApplicationService.sendSmsCode(anyString())).thenReturn(true);

            mockMvc.perform(post(SMS_CODE_URL)
                    .param("phone", "+8613800138000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("手机号格式不合法，返回400")
        void whenInvalidPhoneFormat_thenReturn400() throws Exception {
            mockMvc.perform(post(SMS_CODE_URL)
                    .param("phone", "not-a-phone"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("phone参数缺失，返回400")
        void whenPhoneParamMissing_thenReturn400() throws Exception {
            mockMvc.perform(post(SMS_CODE_URL))
                    .andExpect(status().isBadRequest());
        }
    }
}
