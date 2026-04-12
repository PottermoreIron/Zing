package com.pot.auth.interfaces.rest;

import com.pot.auth.application.service.VerificationCodeApplicationService;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeSendTooFrequentException;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.interfaces.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VerificationCodeController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "pot.ratelimit.enabled=false"
})
@DisplayName("VerificationCodeController slice test")
class VerificationCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationCodeApplicationService verificationCodeApplicationService;

    @Nested
    @DisplayName("POST /auth/code/email")
    class SendEmailCode {

        private static final String EMAIL_CODE_URL = "/auth/code/email";

        @Test
        @DisplayName("Valid email address returns 200 success")
        void whenValidEmail_thenReturn200() throws Exception {
            when(verificationCodeApplicationService.sendEmailCode(anyString())).thenReturn(true);

            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", "test@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Invalid email format returns 400")
        void whenInvalidEmailFormat_thenReturn400() throws Exception {
            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", "not-an-email"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Blank email parameter returns 400")
        void whenEmailBlank_thenReturn400() throws Exception {
            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing email parameter returns 400")
        void whenEmailParamMissing_thenReturn400() throws Exception {
            mockMvc.perform(post(EMAIL_CODE_URL))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Rate limit triggered returns 400 with AUTH_0200 error code")
        void whenCodeSendTooFrequent_thenReturn400WithCode0200() throws Exception {
            when(verificationCodeApplicationService.sendEmailCode(anyString()))
                    .thenThrow(new CodeSendTooFrequentException("Verification code sent too frequently, please try again later"));

            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", "test@example.com"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(AuthResultCode.CODE_SEND_TOO_FREQUENT.getCode()));
        }

        @Test
        @DisplayName("Service returns false, API returns failure response")
        void whenServiceReturnsFalse_thenReturnFailResponse() throws Exception {
            when(verificationCodeApplicationService.sendEmailCode(anyString())).thenReturn(false);

            mockMvc.perform(post(EMAIL_CODE_URL)
                    .param("email", "test@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /auth/code/sms")
    class SendSmsCode {

        private static final String SMS_CODE_URL = "/auth/code/sms";

        @Test
        @DisplayName("Valid phone number returns 200 success")
        void whenValidPhone_thenReturn200() throws Exception {
            when(verificationCodeApplicationService.sendSmsCode(anyString())).thenReturn(true);

            mockMvc.perform(post(SMS_CODE_URL)
                    .param("phone", "+8613800138000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Invalid phone number format returns 400")
        void whenInvalidPhoneFormat_thenReturn400() throws Exception {
            mockMvc.perform(post(SMS_CODE_URL)
                    .param("phone", "not-a-phone"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing phone parameter returns 400")
        void whenPhoneParamMissing_thenReturn400() throws Exception {
            mockMvc.perform(post(SMS_CODE_URL))
                    .andExpect(status().isBadRequest());
        }
    }
}