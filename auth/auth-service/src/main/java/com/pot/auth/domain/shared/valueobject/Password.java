package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;

public record Password(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    public Password {
        if (value == null || value.isBlank()) {
            throw new DomainException(AuthResultCode.WEAK_PASSWORD, "Password must not be blank");
        }

        if (value.length() < MIN_LENGTH) {
            throw new DomainException(AuthResultCode.WEAK_PASSWORD,
                    "Password must be at least " + MIN_LENGTH + " characters");
        }

        if (value.length() > MAX_LENGTH) {
            throw new DomainException(AuthResultCode.WEAK_PASSWORD,
                    "Password must not exceed " + MAX_LENGTH + " characters");
        }

        if (!hasUpperCase(value)) {
            throw new DomainException(AuthResultCode.WEAK_PASSWORD,
                    "Password must contain at least one uppercase letter");
        }

        if (!hasLowerCase(value)) {
            throw new DomainException(AuthResultCode.WEAK_PASSWORD,
                    "Password must contain at least one lowercase letter");
        }

        if (!hasDigit(value)) {
            throw new DomainException(AuthResultCode.WEAK_PASSWORD, "Password must contain at least one digit");
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
