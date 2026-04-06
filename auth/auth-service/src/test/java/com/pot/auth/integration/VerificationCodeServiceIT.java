package com.pot.auth.integration;

import com.pot.auth.application.service.VerificationCodeApplicationService;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeMismatchException;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeNotFoundException;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeSendTooFrequentException;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeVerificationExceededException;
import com.pot.auth.domain.port.CachePort;
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
 * VerificationCodeService 集成测试（使用Testcontainers真实Redis）
 *
 * <p>
 * 在真实Redis环境中验证：
 * <ul>
 * <li>验证码发送后正确写入Redis（带TTL）</li>
 * <li>发送频率限制由Redis状态驱动</li>
 * <li>验证码正确匹配后从Redis删除</li>
 * <li>多次错误尝试后超限异常</li>
 * </ul>
 *
 * <p>
 * Mock清单：
 * <ul>
 * <li>{@link NotificationPort} - 不发真实邮件/短信</li>
 * <li>{@link UserModulePort} - 不连接member-service</li>
 * </ul>
 *
 * @author pot
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("VerificationCodeService 集成测试 (Redis)")
class VerificationCodeServiceIT {

    // Testcontainers - 共享Redis容器（同一JVM进程中复用）

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }

    // 被测对象 & Mock依赖

    @Autowired
    private VerificationCodeApplicationService verificationCodeApplicationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private NotificationPort notificationPort;

    // 阻止MemberModuleAdapter注入（application-test.yml已禁用，此处再次保険）
    @MockitoBean
    private UserModulePort userModulePort;

    // 清理Redis（保证测试隔离）

    @AfterEach
    void cleanRedis() {
        // 清除auth:code:* 和 auth:blacklist:* 键
        var keys = redisTemplate.keys("auth:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // 邮件验证码完整流程

    @Nested
    @DisplayName("邮件验证码 - 完整流程")
    class EmailCodeFlow {

        private static final String EMAIL = "integration@example.com";

        @Test
        @DisplayName("发送验证码 → 从缓存读取 → 验证成功 → 缓存已清除")
        void fullSendAndVerifyFlow() {
            // given: 通知服务模拟返回成功
            when(notificationPort.sendEmailVerificationCode(anyString(), anyString())).thenReturn(true);

            // step1: 发送验证码
            boolean sent = verificationCodeApplicationService.sendEmailCode(EMAIL);
            assertThat(sent).isTrue();

            // step2: 从Redis中读取验证码（验证写入成功）
            String codeKey = "auth:code:" + EMAIL;
            String storedCode = redisTemplate.opsForValue().get(codeKey);
            assertThat(storedCode).isNotNull();
            assertThat(storedCode).matches("^\\d{6}$");

            // step3: 使用正确验证码验证
            boolean verified = verificationCodeApplicationService.verifyCode(EMAIL, storedCode);
            assertThat(verified).isTrue();

            // step4: 验证成功后，缓存应已清除
            assertThat(redisTemplate.hasKey(codeKey)).isFalse();
            assertThat(redisTemplate.hasKey("auth:code:attempts:" + EMAIL)).isFalse();
        }

        @Test
        @DisplayName("1分钟内重复发送，抛出CodeSendTooFrequentException")
        void whenSendTwiceInOneMinnute_thenThrowFrequentException() {
            // given: 首次发送成功
            when(notificationPort.sendEmailVerificationCode(anyString(), anyString())).thenReturn(true);
            verificationCodeApplicationService.sendEmailCode(EMAIL);

            // when & then: 立即再次发送被限频
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
            // given: 发送验证码
            when(notificationPort.sendEmailVerificationCode(anyString(), anyString())).thenReturn(true);
            verificationCodeApplicationService.sendEmailCode(EMAIL);

            // when: 连续错误输入VerificationCode.MAX_ATTEMPTS次
            int maxAttempts = VerificationCode.getMaxAttempts();
            for (int i = 0; i < maxAttempts - 1; i++) {
                assertThatThrownBy(() -> verificationCodeApplicationService.verifyCode(EMAIL, "000000"))
                        .isInstanceOf(CodeMismatchException.class);
            }

            // 最后一次触发超限
            assertThatThrownBy(() -> verificationCodeApplicationService.verifyCode(EMAIL, "000000"))
                    .isInstanceOf(CodeVerificationExceededException.class);

            // 超限后验证码已清除
            assertThat(redisTemplate.hasKey("auth:code:" + EMAIL)).isFalse();
        }

        @Test
        @DisplayName("验证码TTL写入正确（key存在且TTL > 0）")
        void whenCodeSent_thenTtlSetCorrectly() {
            // given
            when(notificationPort.sendEmailVerificationCode(anyString(), anyString())).thenReturn(true);

            // when
            verificationCodeApplicationService.sendEmailCode(EMAIL);

            // then: 验证码key的TTL应在合理范围内（0-300秒）
            Long ttl = redisTemplate.getExpire("auth:code:" + EMAIL);
            assertThat(ttl).isNotNull().isGreaterThan(0L).isLessThanOrEqualTo(VerificationCode.TTL_SECONDS);

            // 发送限制key的TTL应在合理范围内（0-60秒）
            Long sendLimitTtl = redisTemplate.getExpire("auth:code:send:" + EMAIL);
            assertThat(sendLimitTtl).isNotNull().isGreaterThan(0L).isLessThanOrEqualTo(60L);
        }
    }
}
