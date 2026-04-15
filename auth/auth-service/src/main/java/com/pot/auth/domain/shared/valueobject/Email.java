package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new DomainException(AuthResultCode.INVALID_EMAIL, "Email address must not be blank");
        }

        String trimmed = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new DomainException(AuthResultCode.INVALID_EMAIL, "Invalid email address format: " + value);
        }

        value = trimmed;
    }

    public static Email of(String value) {
        return new Email(value);
    }

    public String getDomain() {
        int atIndex = value.indexOf('@');
        return value.substring(atIndex + 1);
    }

    public String getLocalPart() {
        int atIndex = value.indexOf('@');
        return value.substring(0, atIndex);
    }

    public boolean isCorporateEmail() {
        String domain = getDomain();
        return !domain.equals("gmail.com")
                && !domain.equals("qq.com")
                && !domain.equals("163.com")
                && !domain.equals("126.com")
                && !domain.equals("outlook.com")
                && !domain.equals("hotmail.com");
    }
}
