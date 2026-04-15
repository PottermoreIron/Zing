package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.exception.DomainException;
import lombok.Builder;

import java.security.SecureRandom;

@Builder
public record VerificationCode(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;

        public VerificationCode {
        if (value == null || value.isBlank()) {
            throw new InvalidVerificationCodeException("Verification code must not be blank");
        }
        if (!value.matches("^\\d{" + CODE_LENGTH + "}$")) {
            throw new InvalidVerificationCodeException("Verification code must be " + CODE_LENGTH + " digits");
        }
    }

        public static VerificationCode of(String value) {
        return new VerificationCode(value);
    }

        public static VerificationCode generate() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        int code = RANDOM.nextInt(bound);
        String codeStr = String.format("%0" + CODE_LENGTH + "d", code);
        return new VerificationCode(codeStr);
    }

        public boolean matches(String inputCode) {
        return this.value.equals(inputCode);
    }

        public static class InvalidVerificationCodeException extends DomainException {
        public InvalidVerificationCodeException(String message) {
            super(message);
        }
    }
}
