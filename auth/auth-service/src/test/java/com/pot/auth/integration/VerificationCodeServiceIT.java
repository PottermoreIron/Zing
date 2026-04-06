package com.pot.auth.integration;

import com.pot.auth.application.service.VerificationCodeApplicationService;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeMismatchException;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeNotFoundException;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeSendTooFrequentException;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeVerificationExceededException;
import com.pot.auth.domain.port.NotificationPort;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for VerificationCodeService backed by a real Redis
 * container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("VerificationCodeService 集成测试 (Redis)")
class VerificationCodeServiceIT {

    // Share one Redis container across the test JVM.

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }

    @Autowired
    private VerificationCodeApplicationService verificationCodeApplicationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private NotificationPort notificationPort;

    // Keep the member adapter out of the test context even if profile settings
    // change.
    @MockitoBean
    private UserModulePort userModulePort;

    @AfterEach
    void cleanRedis() {
        var keys = redisTemplate.keys("auth:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Nested
    @DisplayName("邮件验证码 - 完整流程")
    class EmailCodeFlow {

        private static final String EMAIL = "integration@example.com";

        @Test
        @DisplayName("发送验证码 → 从缓存读取 → 验证成功 → 缓存已清除")
        void fullSendAndVerifyFlow() {
            when(notificationPort.sendEmailVerificationCode(anyString(), anyString())).thenReturn(true);
            boolean sent = verificationCodeApplicationService.sendEmailCode(EMAIL);
            assertThat(sent).isTrue();
            String codeKey = "auth:code:" + EMAIL;
            String storedCode = redisTemplate.opsForValue().get(codeKey);
            assertThat(storedCode).isNotNull();
            assertThat(storedCode).matches("^\\d{6}$");
            boolean verified = verificationCodeApplicationService.verifyCode(EMAIL, storedCode);
            assertThat(verified).isTrue();
            assertThat(redisTemplate.hasKey(codeKey)).isFalse();
            assertThat(redisTemplate.hasKey("auth:code:attempts:" + EMAIL)).isFalse();
        }

        @Test
        @DisplayName("1分钟内重复发送，抛出CodeSendTooFrequentException")
        void whenSendTwiceInOneMinnute_thenThrowFrequentException() {
            when(notificationPort.sendEmailVerificationCode(anyString(), anyString())).thenReturn(true);
            verificationCodeApplicationService.sendEmailCode(EMAIL);

            assertThatThrownBy(() -> verificationCodeApplicationService.sendEmailCode(EMAIL))
                    .isInstanceOf(CodeSendTooFrequentException.class);
        }

        @Test
        @DisplayName("验证码不存在（从未发送），抛出CodeNotFoundException")
        void whenCodeNeverSent_thenThrowCodeNotFoundException() {
            assertThatThrownBy(() -> verificationCodeApplicationService.verifyCode("nosend@example.com", "123456"))
                    .isInstanceOf(CodeNotFoundException.class);
        }

        @Test
        @DisplayName("连续输入错误验证码3次后，抛出CodeVerificationExceededException")
        void whenExceedMaxAttempts_thenThrowExceededException() {
            when(notificationPort.sendEmailVerificationCode(anyString(), anyString())).thenReturn(true);
            verificationCodeApplicationService.sendEmailCode(EMAIL);

            int maxAttempts = VerificationCode.getMaxAttempts();
            for (int i = 0; i < maxAttempts - 1; i++) {
                assertThatThrownBy(() -> verificationCodeApplicationService.verifyCode(EMAIL, "000000"))
                        .isInstanceOf(CodeMismatchException.class);
            }

            assertThatThrownBy(() -> verificationCodeApplicationService.verifyCode(EMAIL, "000000"))
                    .isInstanceOf(CodeVerificationExceededException.class);
            assertThat(redisTemplate.hasKey("auth:code:" + EMAIL)).isFalse();
        }

        @Test
        @DisplayName("验证码TTL写入正确（key存在且TTL > 0）")
        void whenCodeSent_thenTtlSetCorrectly() {
            when(notificationPort.sendEmailVerificationCode(anyString(), anyString())).thenReturn(true);

            verificationCodeApplicationService.sendEmailCode(EMAIL);

            Long ttl = redisTemplate.getExpire("auth:code:" + EMAIL);
            assertThat(ttl).isNotNull().isGreaterThan(0L).isLessThanOrEqualTo(VerificationCode.TTL_SECONDS);

            // 发送限制key的TTL应在合理范围内（0-60秒）
            Long sendLimitTtl = redisTemplate.getExpire("auth:code:send:" + EMAIL);
            assertThat(sendLimitTtl).isNotNull().isGreaterThan(0L).isLessThanOrEqualTo(60L);
        }
    }
}
