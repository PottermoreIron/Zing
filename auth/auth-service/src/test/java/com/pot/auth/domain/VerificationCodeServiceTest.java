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

        // Bypass lock coordination so these tests stay focused on verification-code
        // behavior.
        org.mockito.stubbing.Stubber stubber = lenient().doAnswer(invocation -> {
            Supplier<?> task = invocation.getArgument(4);
            return task.get();
        });
        stubber.when(distributedLockPort).executeWithLock(
                anyString(), anyLong(), anyLong(), any(), org.mockito.ArgumentMatchers.<Supplier<Object>>any());
    }

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
            when(cachePort.exists(sendLimitKey)).thenReturn(true);

            assertThatThrownBy(() -> verificationCodeService.sendEmailVerificationCode(new Email(email)))
                    .isInstanceOf(CodeSendTooFrequentException.class)
                    .hasMessageContaining("频繁");
            verifyNoInteractions(distributedLockPort, notificationPort);
        }

        @Test
        @DisplayName("首次发送邮件验证码，写缓存并调用通知接口")
        void whenFirstSend_thenStoreCacheAndCallNotification() {
            when(cachePort.exists(sendLimitKey)).thenReturn(false);
            when(notificationPort.sendEmailVerificationCode(eq(email), anyString())).thenReturn(true);

            boolean result = verificationCodeService.sendEmailVerificationCode(new Email(email));

            assertThat(result).isTrue();
            verify(cachePort).set(eq(codeKey), anyString(), eq(POLICY.codeTtl()));
            verify(cachePort).set(eq(attemptsKey), eq("0"), eq(POLICY.codeTtl()));
            verify(cachePort).set(eq(sendLimitKey), eq("1"), eq(POLICY.sendCooldown()));
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationPort).sendEmailVerificationCode(eq(email), codeCaptor.capture());
            assertThat(codeCaptor.getValue()).matches("^\\d{6}$");
        }

        @Test
        @DisplayName("发送失败时（通知接口返回false），返回false但不抛异常")
        void whenNotificationFails_thenReturnFalse() {
            when(cachePort.exists(sendLimitKey)).thenReturn(false);
            when(notificationPort.sendEmailVerificationCode(eq(email), anyString())).thenReturn(false);

            boolean result = verificationCodeService.sendEmailVerificationCode(new Email(email));

            assertThat(result).isFalse();
        }
    }

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
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.of(storedCode));
            when(cachePort.get(attemptsKey, String.class)).thenReturn(Optional.of("0"));

            boolean result = verificationCodeService.verifyCode(recipient, storedCode);

            assertThat(result).isTrue();
            verify(cachePort).delete(codeKey);
            verify(cachePort).delete(attemptsKey);
        }

        @Test
        @DisplayName("验证码不存在（已过期或未发送），抛出CodeNotFoundException")
        void whenCodeNotFound_thenThrowCodeNotFoundException() {
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> verificationCodeService.verifyCode(recipient, storedCode))
                    .isInstanceOf(CodeNotFoundException.class)
                    .hasMessageContaining("过期");
        }

        @Test
        @DisplayName("验证码错误，增加尝试次数，抛出CodeMismatchException")
        void whenCodeMismatch_thenIncrementAttemptsAndThrow() {
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.of(storedCode));
            when(cachePort.get(attemptsKey, String.class)).thenReturn(Optional.of("1"));

            assertThatThrownBy(() -> verificationCodeService.verifyCode(recipient, "999999"))
                    .isInstanceOf(CodeMismatchException.class)
                    .hasMessageContaining("错误");

            verify(cachePort).set(eq(attemptsKey), eq("2"), any(Duration.class));
        }

        @Test
        @DisplayName("验证次数达到上限（3次），抛出CodeVerificationExceededException并清除缓存")
        void whenAttemptsExceedLimit_thenThrowAndClearCache() {
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.of(storedCode));
            when(cachePort.get(attemptsKey, String.class))
                    .thenReturn(Optional.of(String.valueOf(POLICY.maxAttempts())));

            assertThatThrownBy(() -> verificationCodeService.verifyCode(recipient, storedCode))
                    .isInstanceOf(CodeVerificationExceededException.class)
                    .hasMessageContaining("超限");

            verify(cachePort).delete(codeKey);
            verify(cachePort).delete(attemptsKey);
        }

        @Test
        @DisplayName("尝试次数为null时，视为0次，正常处理")
        void whenAttemptsKeyMissing_thenTreatAsZero() {
            when(cachePort.get(codeKey, String.class)).thenReturn(Optional.of(storedCode));
            when(cachePort.get(attemptsKey, String.class)).thenReturn(Optional.empty());

            boolean result = verificationCodeService.verifyCode(recipient, storedCode);

            assertThat(result).isTrue();
        }
    }
}
