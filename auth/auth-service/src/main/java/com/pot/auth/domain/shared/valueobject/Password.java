package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.exception.WeakPasswordException;

/**
 * 密码值对象 (Domain Primitive)
 *
 * <p>封装密码的复杂度规则和业务行为
 * <ul>
 *   <li>最少8位</li>
 *   <li>必须包含大小写字母和数字</li>
 *   <li>可选：特殊字符</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
public record Password(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    /**
     * 紧凑构造器 - 自动验证
     */
    public Password {
        if (value == null || value.isBlank()) {
            throw new WeakPasswordException("密码不能为空");
        }

        if (value.length() < MIN_LENGTH) {
            throw new WeakPasswordException("密码至少" + MIN_LENGTH + "位");
        }

        if (value.length() > MAX_LENGTH) {
            throw new WeakPasswordException("密码最多" + MAX_LENGTH + "位");
        }

        if (!hasUpperCase(value)) {
            throw new WeakPasswordException("密码必须包含大写字母");
        }

        if (!hasLowerCase(value)) {
            throw new WeakPasswordException("密码必须包含小写字母");
        }

        if (!hasDigit(value)) {
            throw new WeakPasswordException("密码必须包含数字");
        }
    }

    /**
     * 从字符串创建密码值对象
     */
    public static Password of(String value) {
        return new Password(value);
    }

    /**
     * 是否包含大写字母
     */
    private static boolean hasUpperCase(String password) {
        return password.chars().anyMatch(Character::isUpperCase);
    }

    /**
     * 是否包含小写字母
     */
    private static boolean hasLowerCase(String password) {
        return password.chars().anyMatch(Character::isLowerCase);
    }

    /**
     * 是否包含数字
     */
    private static boolean hasDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }

    /**
     * 是否包含特殊字符
     */
    private static boolean hasSpecialChar(String password) {
        return password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
    }

    /**
     * 计算密码强度 (0-100)
     */
    public int calculateStrength() {
        int strength = 0;

        // 基础分：长度
        strength += Math.min(value.length() * 4, 40);

        // 包含大写字母
        if (hasUpperCase(value)) {
            strength += 15;
        }

        // 包含小写字母
        if (hasLowerCase(value)) {
            strength += 15;
        }

        // 包含数字
        if (hasDigit(value)) {
            strength += 15;
        }

        // 包含特殊字符
        if (hasSpecialChar(value)) {
            strength += 15;
        }

        return Math.min(strength, 100);
    }
}

