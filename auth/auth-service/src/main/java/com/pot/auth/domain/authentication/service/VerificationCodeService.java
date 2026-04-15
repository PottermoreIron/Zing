package com.pot.auth.domain.authentication.service;

import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.DistributedLockPort;
import com.pot.auth.domain.port.NotificationPort;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Phone;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class VerificationCodeService {

    private final CachePort cachePort;
    private final NotificationPort notificationPort;
    private final DistributedLockPort distributedLockPort;
    private final VerificationCodePolicy policy;

    public VerificationCodeService(
            CachePort cachePort,
            NotificationPort notificationPort,
            DistributedLockPort distributedLockPort,
            VerificationCodePolicy policy) {
        this.cachePort = cachePort;
        this.notificationPort = notificationPort;
        this.distributedLockPort = distributedLockPort;
        this.policy = policy;
    }

        public boolean sendEmailVerificationCode(Email email) {
        log.info("[Code] Sending email verification code — email={}", email.value());

        String recipient = email.value();
        String lockKey = policy.lockKey(recipient);
        return distributedLockPort.executeWithLock(
                lockKey,
                policy.lockWaitSeconds(),
                policy.lockLeaseSeconds(),
                TimeUnit.SECONDS,
                () -> {
                    String sendLimitKey = policy.sendLimitKey(recipient);
                    if (cachePort.exists(sendLimitKey)) {
                        log.warn("[Code] Send rate exceeded — email={}", email.value());
                        throw new CodeSendTooFrequentException("Verification code sent too frequently, please try again later");
                    }

                    VerificationCode code = VerificationCode.generate();

                    String codeKey = policy.codeKey(recipient);
                    cachePort.set(codeKey, code.value(), policy.codeTtl());

                    String attemptsKey = policy.attemptsKey(recipient);
                    cachePort.set(attemptsKey, "0", policy.codeTtl());

                    cachePort.set(sendLimitKey, "1", policy.sendCooldown());

                    boolean sent = notificationPort.sendEmailVerificationCode(email.value(), code.value());

                    if (sent) {
                        log.info("[Code] Email code sent — email={}", email.value());
                    } else {
                        log.error("[Code] Failed to send email code — email={}", email.value());
                    }

                    return sent;
                });
    }

        public boolean sendSmsVerificationCode(Phone phoneNumber) {
        log.info("[Code] Sending SMS verification code — phone={}", phoneNumber.value());

        String recipient = phoneNumber.value();
        String lockKey = policy.lockKey(recipient);
        return distributedLockPort.executeWithLock(
                lockKey,
                policy.lockWaitSeconds(),
                policy.lockLeaseSeconds(),
                TimeUnit.SECONDS,
                () -> {
                    String sendLimitKey = policy.sendLimitKey(recipient);
                    if (cachePort.exists(sendLimitKey)) {
                        log.warn("[Code] Send rate exceeded — phone={}", phoneNumber.value());
                        throw new CodeSendTooFrequentException("Verification code sent too frequently, please try again later");
                    }

                    VerificationCode code = VerificationCode.generate();

                    String codeKey = policy.codeKey(recipient);
                    cachePort.set(codeKey, code.value(), policy.codeTtl());

                    String attemptsKey = policy.attemptsKey(recipient);
                    cachePort.set(attemptsKey, "0", policy.codeTtl());

                    cachePort.set(sendLimitKey, "1", policy.sendCooldown());

                    boolean sent = notificationPort.sendSmsVerificationCode(phoneNumber.value(), code.value());

                    if (sent) {
                        log.info("[Code] SMS code sent — phone={}", phoneNumber.value());
                    } else {
                        log.error("[Code] Failed to send SMS code — phone={}", phoneNumber.value());
                    }

                    return sent;
                });
    }

        public boolean verifyCode(String recipient, String inputCode) {
        log.info("[Code] Verifying code — recipient={}", recipient);

        String lockKey = "lock:verify:code:" + recipient;
        return distributedLockPort.executeWithLock(
                lockKey,
                policy.lockWaitSeconds(),
                policy.lockLeaseSeconds(),
                TimeUnit.SECONDS,
                () -> {
                    String codeKey = policy.codeKey(recipient);
                    String storedCode = cachePort.get(codeKey, String.class).orElse(null);

                    if (storedCode == null) {
                        log.warn("[Code] Code not found or expired — recipient={}", recipient);
                        throw new CodeNotFoundException("Verification code not found or expired");
                    }

                    String attemptsKey = policy.attemptsKey(recipient);
                    String attemptsStr = cachePort.get(attemptsKey, String.class).orElse(null);
                    int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

                    if (attempts >= policy.maxAttempts()) {
                        log.warn("[Code] Verification attempt limit reached — recipient={}, attempts={}", recipient, attempts);
                        cachePort.delete(codeKey);
                        cachePort.delete(attemptsKey);
                        throw new CodeVerificationExceededException("Verification attempt limit exceeded, please request a new code");
                    }

                    VerificationCode code = new VerificationCode(storedCode);
                    boolean isValid = code.matches(inputCode);

                    if (isValid) {
                        log.info("[Code] Verification passed — recipient={}", recipient);
                        cachePort.delete(codeKey);
                        cachePort.delete(attemptsKey);
                        return true;
                    } else {
                        log.warn("[Code] Verification failed — recipient={}, attempts={}", recipient, attempts + 1);
                        cachePort.set(attemptsKey, String.valueOf(attempts + 1), policy.codeTtl());
                        throw new CodeMismatchException("Incorrect verification code");
                    }
                });
    }

        public boolean verifyCode(String recipient, VerificationCode inputCode) {
        return verifyCode(recipient, inputCode.value());
    }

        public void deleteCode(String recipient) {
        String codeKey = policy.codeKey(recipient);
        String attemptsKey = policy.attemptsKey(recipient);
        cachePort.delete(codeKey);
        cachePort.delete(attemptsKey);
        log.info("[Code] Code deleted — recipient={}", recipient);
    }

        public static class CodeSendTooFrequentException extends DomainException {
        public CodeSendTooFrequentException(String message) {
            super(message);
        }
    }

        public static class CodeNotFoundException extends DomainException {
        public CodeNotFoundException(String message) {
            super(message);
        }
    }

        public static class CodeMismatchException extends DomainException {
        public CodeMismatchException(String message) {
            super(message);
        }
    }

        public static class CodeVerificationExceededException extends DomainException {
        public CodeVerificationExceededException(String message) {
            super(message);
        }
    }
}
