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

/**
 * 验证码领域服务
 *
 * <p>
 * 负责验证码的生命周期管理：
 * <ul>
 * <li>生成验证码</li>
 * <li>发送验证码（邮件/短信）</li>
 * <li>验证验证码</li>
 * <li>尝试次数限制</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-10
 */
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

    /**
     * 发送邮件验证码
     *
     * @param email 邮箱
     * @return 是否发送成功
     */
    public boolean sendEmailVerificationCode(Email email) {
        log.info("[验证码] 发送邮件验证码: email={}", email.value());

        String recipient = email.value();
        String sendLimitKey = policy.sendLimitKey(recipient);
        if (cachePort.exists(sendLimitKey)) {
            log.warn("[验证码] 发送过于频繁: email={}", email.value());
            throw new CodeSendTooFrequentException("验证码发送过于频繁，请稍后再试");
        }

        String lockKey = policy.lockKey(recipient);
        return distributedLockPort.executeWithLock(
                lockKey,
                policy.lockWaitSeconds(),
                policy.lockLeaseSeconds(),
                TimeUnit.SECONDS,
                () -> {
                    VerificationCode code = VerificationCode.generate();

                    String codeKey = policy.codeKey(recipient);
                    cachePort.set(codeKey, code.value(), policy.codeTtl());

                    String attemptsKey = policy.attemptsKey(recipient);
                    cachePort.set(attemptsKey, "0", policy.codeTtl());

                    cachePort.set(sendLimitKey, "1", policy.sendCooldown());

                    boolean sent = notificationPort.sendEmailVerificationCode(email.value(), code.value());

                    if (sent) {
                        log.info("[验证码] 邮件验证码发送成功: email={}", email.value());
                    } else {
                        log.error("[验证码] 邮件验证码发送失败: email={}", email.value());
                    }

                    return sent;
                });
    }

    /**
     * 发送短信验证码
     *
     * @param phoneNumber 手机号
     * @return 是否发送成功
     */
    public boolean sendSmsVerificationCode(Phone phoneNumber) {
        log.info("[验证码] 发送短信验证码: phone={}", phoneNumber.value());

        String recipient = phoneNumber.value();
        String sendLimitKey = policy.sendLimitKey(recipient);
        if (cachePort.exists(sendLimitKey)) {
            log.warn("[验证码] 发送过于频繁: phone={}", phoneNumber.value());
            throw new CodeSendTooFrequentException("验证码发送过于频繁，请稍后再试");
        }

        String lockKey = policy.lockKey(recipient);
        return distributedLockPort.executeWithLock(
                lockKey,
                policy.lockWaitSeconds(),
                policy.lockLeaseSeconds(),
                TimeUnit.SECONDS,
                () -> {
                    VerificationCode code = VerificationCode.generate();

                    String codeKey = policy.codeKey(recipient);
                    cachePort.set(codeKey, code.value(), policy.codeTtl());

                    String attemptsKey = policy.attemptsKey(recipient);
                    cachePort.set(attemptsKey, "0", policy.codeTtl());

                    cachePort.set(sendLimitKey, "1", policy.sendCooldown());

                    boolean sent = notificationPort.sendSmsVerificationCode(phoneNumber.value(), code.value());

                    if (sent) {
                        log.info("[验证码] 短信验证码发送成功: phone={}", phoneNumber.value());
                    } else {
                        log.error("[验证码] 短信验证码发送失败: phone={}", phoneNumber.value());
                    }

                    return sent;
                });
    }

    /**
     * 验证验证码
     *
     * @param recipient 接收者（邮箱或手机号）
     * @param inputCode 输入的验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String recipient, String inputCode) {
        log.info("[验证码] 验证验证码: recipient={}", recipient);

        String codeKey = policy.codeKey(recipient);
        String storedCode = cachePort.get(codeKey, String.class).orElse(null);

        if (storedCode == null) {
            log.warn("[验证码] 验证码不存在或已过期: recipient={}", recipient);
            throw new CodeNotFoundException("验证码不存在或已过期");
        }

        String attemptsKey = policy.attemptsKey(recipient);
        String attemptsStr = cachePort.get(attemptsKey, String.class).orElse(null);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= policy.maxAttempts()) {
            log.warn("[验证码] 验证次数超限: recipient={}, attempts={}", recipient, attempts);
            cachePort.delete(codeKey);
            cachePort.delete(attemptsKey);
            throw new CodeVerificationExceededException("验证次数超限，请重新获取验证码");
        }

        VerificationCode code = new VerificationCode(storedCode);
        boolean isValid = code.matches(inputCode);

        if (isValid) {
            log.info("[验证码] 验证成功: recipient={}", recipient);
            cachePort.delete(codeKey);
            cachePort.delete(attemptsKey);
            return true;
        } else {
            log.warn("[验证码] 验证失败: recipient={}, attempts={}", recipient, attempts + 1);
            cachePort.set(attemptsKey, String.valueOf(attempts + 1), policy.codeTtl());
            throw new CodeMismatchException("验证码错误");
        }
    }

    /**
     * 验证验证码（VerificationCode对象版本）
     */
    public boolean verifyCode(String recipient, VerificationCode inputCode) {
        return verifyCode(recipient, inputCode.value());
    }

    /**
     * 删除验证码
     */
    public void deleteCode(String recipient) {
        String codeKey = policy.codeKey(recipient);
        String attemptsKey = policy.attemptsKey(recipient);
        cachePort.delete(codeKey);
        cachePort.delete(attemptsKey);
        log.info("[验证码] 已删除验证码: recipient={}", recipient);
    }

    /**
     * 验证码发送过于频繁异常
     */
    public static class CodeSendTooFrequentException extends DomainException {
        public CodeSendTooFrequentException(String message) {
            super(message);
        }
    }

    /**
     * 验证码不存在异常
     */
    public static class CodeNotFoundException extends DomainException {
        public CodeNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * 验证码错误异常
     */
    public static class CodeMismatchException extends DomainException {
        public CodeMismatchException(String message) {
            super(message);
        }
    }

    /**
     * 验证次数超限异常
     */
    public static class CodeVerificationExceededException extends DomainException {
        public CodeVerificationExceededException(String message) {
            super(message);
        }
    }
}
