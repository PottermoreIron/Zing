package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.exception.DomainException;
import lombok.Builder;

import java.security.SecureRandom;

/**
 * 验证码值对象
 *
 * <p>业务规则：
 * <ul>
 *   <li>必须是6位数字</li>
 *   <li>5分钟有效期</li>
 *   <li>最多3次验证尝试</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-10
 */
@Builder
public record VerificationCode(String value) {

    public static final int TTL_SECONDS = 300; // 5分钟
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 验证验证码格式
     */
    public VerificationCode {
        if (value == null || value.isBlank()) {
            throw new InvalidVerificationCodeException("验证码不能为空");
        }
        if (!value.matches("^\\d{6}$")) {
            throw new InvalidVerificationCodeException("验证码必须是6位数字");
        }
    }

    /**
     * 静态工厂方法
     */
    public static VerificationCode of(String value) {
        return new VerificationCode(value);
    }

    /**
     * 生成新的验证码
     */
    public static VerificationCode generate() {
        int code = RANDOM.nextInt(1000000);
        String codeStr = String.format("%06d", code);
        return new VerificationCode(codeStr);
    }

    /**
     * 获取最大尝试次数
     */
    public static int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    /**
     * 验证码是否匹配
     */
    public boolean matches(String inputCode) {
        return this.value.equals(inputCode);
    }

    /**
     * 无效验证码异常
     */
    public static class InvalidVerificationCodeException extends DomainException {
        public InvalidVerificationCodeException(String message) {
            super(message);
        }
    }
}

