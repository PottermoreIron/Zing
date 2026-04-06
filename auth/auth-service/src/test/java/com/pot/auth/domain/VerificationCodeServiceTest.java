package com.pot.auth.domain;

import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeMismatchException;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeNotFoundException;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeSendTooFrequentException;
import com.pot.auth.domain.authentication.service.VerificationCodeService.CodeVerificationExceededException;
import com.pot.auth.domain.authentication.service.VerificationCodePolicy;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.DistributedLockPort;
import com.pot.auth.domain.port.NotificationPort;
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Phone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VerificationCodeService 单元测试
 *
 * <p>
 * 覆盖验证码发送和校验的全部业务分支：
 * <ul>
 * <li>发送频率限制</li>
 * <li>发送成功后的缓存写入</li>
 * <li>验证码正确匹配</li>
 * <li>验证码错误计数与超限</li>
 * <li>验证码不存在（已过期或未发送）</li>
 * </ul>
 *
 * @author pot
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationCodeService 单元测试")
class VerificationCodeServiceTest {

    private static final VerificationCodePolicy POLICY = new VerificationCodePolicy(
            "auth:code:",
            "auth:code:attempts:",
            "auth:code:send:",
            300,
            3,
            60,
            3,
            10);

    @Mock
    private CachePort cachePort;

    @Mock
    private NotificationPort notificationPort;

    @Mock
    private DistributedLockPort distributedLockPort;

    private VerificationCodeService verificationCodeService;

    @BeforeEach
    void setUp() {
        verificationCodeService = new VerificationCodeService(cachePort, notificationPort, distributedLockPort, POLICY);

        // 默认让分布式锁直接执行任务（穿透锁逻辑，专注业务测试）
        org.mockito.stubbing.Stubber stubber = lenient().doAnswer(invocation -> {
            Supplier<?> task = invocation.getArgument(4);
            return task.get();
        });
        stubber.when(distributedLockPort).executeWithLock(
                anyString(), anyLong(), anyLong(), any(), org.mockito.ArgumentMatchers.<Supplier<Object>>any());
    }

    // 邮件验证码发送

    @Nested
    @DisplayName("发送邮件验证码")
    class SendEmailVerificationCode {

        private final String email = "test@example.com";
        private final String sendLimitKey = "auth:code:send:" + email;
        private final String codeKey = "auth:code:" + email;
        private final String attemptsKey = "auth:code:attempts:" + email;

        @Test
        @DisplayName("当1分钟内重复发送时，抛出CodeSendTooFrequentException")
        void whenFreqLimitActive_thenThrowCodeSendTooFrequentException() {
            // given: 发送限制KEY已存在（1分钟内已发送过）
            when(cachePort.exists(sendLimitKey)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> verificationCodeService.sendEmailVerificationCode(new Email(email)))
                    .isInstanceOf(CodeSendTooFrequentException.class)
                    .hasMessageContaining("频繁");

            // 未触发分布式锁和通知
            verifyNoInteractions(distributedLockPort, notificationPort);
        }

        @Test
        @DisplayName("首次发送邮件验证码，写缓存并调用通知接口")
        void whenFirstSend_thenStoreCacheAndCallNotification() {
            // given: 无频率限制
            when(cachePort.exists(sendLimitKey)).thenReturn(false);
            when(notificationPort.sendEmailVerificationCode(eq(email), anyString())).thenReturn(true);

            // when
            boolean result = verificationCodeService.sendEmailVerificationCode(new Email(email));

            // then
            assertThat(result).isTrue();

            // 验证验证码写入缓存（5分钟TTL）
            verify(cachePort).set(eq(codeKey), anyString(), eq(POLICY.codeTtl()));

            // 验证尝试次数初始化
            verify(cachePort).set(eq(attemptsKey), eq("0"), eq(POLICY.codeTtl()));

            // 验证频率限制KEY写入（1分钟TTL）
            verify(cachePort).set(eq(sendLimitKey), eq("1"), eq(POLICY.sendCooldown()));

            // 验证通知接口被调用
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationPort).sendEmailVerificationCode(eq(email), codeCaptor.capture());
            assertThat(codeCaptor.getValue()).matches("^\\d{6}$");
        }

        @Test
        @DisplayName("发送失败时（通知接口返回false），返回false但不抛异常")
        void whenNotificationFails_thenReturnFalse() {
            // given
            when(cachePort.exists(sendLimitKey)).thenReturn(false);
            when(notificationPort.sendEmailVerificationCode(eq(email), anyString())).thenReturn(false);

            // when
            boolean result = verificationCodeService.sendEmailVerificationCode(new Email(email));

            // then: 返回false而不是抛出异常
            assertThat(result).isFalse();
        }
    }

    // 短信验证码发送

    @Nested
    @DisplayName("发送短信验证码")
    class SendSmsVerificationCode {

        private final String phone = "+8613800138000";
        private final String sendLimitKey = "auth:code:send:" + phone;

        @Test
        @DisplayName("短信发送频率限制，抛出CodeSendTooFrequentException")
        void whenFreqLimitActive_thenThrowCodeSendTooFrequentException() {
            when(cachePort.exists(sendLimitKey)).thenReturn(true);

            assertThatThrownBy(() -> verificationCodeService.sendSmsVerificationCode(new Phone(phone)))
                    .isInstanceOf(CodeSendTooFrequentException.class);
        }

        @Test
        @DisplayName("短信首次发送成功，调用通知接口")
        void whenFirstSend_thenCallSmsNotification() {
            when(cachePort.exists(sendLimitKey)).thenReturn(false);
            when(notificationPort.sendSmsVerificationCode(eq(phone), anyString())).thenReturn(true);

            boolean result = verificationCodeService.sendSmsVerificationCode(new Phone(phone));

            assertThat(result).isTrue();
            verify(notificationPort).sendSmsVerificationCode(eq(phone), anyString());
        }
    }

    // 验证码校验

    @Nested
    @DisplayName("验证码校验")
    class VerifyCode {

        private final String recipient = "test@example.com";
        private final String storedCode = "123456";
        private final String codeKey = "auth:code:" + recipient;
        private final String attemptsKey = "auth:code:attempts:" + recipient;

        @Test
        @DisplayName("验证码正确，返回true并清除缓存")
        void whenCodeMatches_thenReturnTrueAndDeleteCache() {
            // given
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.of(storedCode));
            when(cachePort.get(attemptsKey, String.class)).thenReturn(Optional.of("0"));

            // when
            boolean result = verificationCodeService.verifyCode(recipient, storedCode);

            // then
            assertThat(result).isTrue();
            verify(cachePort).delete(codeKey);
            verify(cachePort).delete(attemptsKey);
        }

        @Test
        @DisplayName("验证码不存在（已过期或未发送），抛出CodeNotFoundException")
        void whenCodeNotFound_thenThrowCodeNotFoundException() {
            // given: 缓存中没有验证码
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> verificationCodeService.verifyCode(recipient, storedCode))
                    .isInstanceOf(CodeNotFoundException.class)
                    .hasMessageContaining("过期");
        }

        @Test
        @DisplayName("验证码错误，增加尝试次数，抛出CodeMismatchException")
        void whenCodeMismatch_thenIncrementAttemptsAndThrow() {
            // given
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.of(storedCode));
            when(cachePort.get(attemptsKey, String.class)).thenReturn(Optional.of("1"));

            // when & then
            assertThatThrownBy(() -> verificationCodeService.verifyCode(recipient, "999999"))
                    .isInstanceOf(CodeMismatchException.class)
                    .hasMessageContaining("错误");

            // 尝试次数应递增至2
            verify(cachePort).set(eq(attemptsKey), eq("2"), any(Duration.class));
        }

        @Test
        @DisplayName("验证次数达到上限（3次），抛出CodeVerificationExceededException并清除缓存")
        void whenAttemptsExceedLimit_thenThrowAndClearCache() {
            // given: 已尝试MAX_ATTEMPTS次
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.of(storedCode));
            when(cachePort.get(attemptsKey, String.class))
                    .thenReturn(Optional.of(String.valueOf(POLICY.maxAttempts())));

            // when & then
            assertThatThrownBy(() -> verificationCodeService.verifyCode(recipient, storedCode))
                    .isInstanceOf(CodeVerificationExceededException.class)
                    .hasMessageContaining("超限");

            // 验证缓存已清除
            verify(cachePort).delete(codeKey);
            verify(cachePort).delete(attemptsKey);
        }

        @Test
        @DisplayName("尝试次数为null时，视为0次，正常处理")
        void whenAttemptsKeyMissing_thenTreatAsZero() {
            // given: 尝试次数KEY不存在（返回empty）
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.of(storedCode));
            when(cachePort.get(attemptsKey, String.class)).thenReturn(Optional.empty());

            // when
            boolean result = verificationCodeService.verifyCode(recipient, storedCode);

            // then: 正常通过
            assertThat(result).isTrue();
        }
    }
}
