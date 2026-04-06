package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.exception.WeakPasswordException;

public record Password(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

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

        public static Password of(String value) {
        return new Password(value);
    }

        private static boolean hasUpperCase(String password) {
        return password.chars().anyMatch(Character::isUpperCase);
    }

        private static boolean hasLowerCase(String password) {
        return password.chars().anyMatch(Character::isLowerCase);
    }

        private static boolean hasDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }

        private static boolean hasSpecialChar(String password) {
        return password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
    }

        public int calculateStrength() {
        int strength = 0;

        strength += Math.min(value.length() * 4, 40);

        if (hasUpperCase(value)) {
            strength += 15;
        }

        if (hasLowerCase(value)) {
            strength += 15;
        }

        if (hasDigit(value)) {
            strength += 15;
        }

        if (hasSpecialChar(value)) {
            strength += 15;
        }

        return Math.min(strength, 100);
    }
}

