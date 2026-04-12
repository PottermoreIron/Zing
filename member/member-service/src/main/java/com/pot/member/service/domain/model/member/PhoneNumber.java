package com.pot.member.service.domain.model.member;

import lombok.Value;

@Value
public class PhoneNumber {
    String value;

    public PhoneNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be blank");
        }
        String cleanedPhone = value.trim().replaceAll("\\s+", "");
        if (!isValidPhone(cleanedPhone)) {
            throw new IllegalArgumentException("Invalid phone number format: " + value);
        }
        this.value = cleanedPhone;
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    private boolean isValidPhone(String phone) {
        String phoneRegex = "^(\\+?86)?1[3-9]\\d{9}$";
        return phone.matches(phoneRegex);
    }
}
