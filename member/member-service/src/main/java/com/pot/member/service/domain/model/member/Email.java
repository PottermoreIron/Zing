package com.pot.member.service.domain.model.member;

import lombok.Value;

/**
 * 邮箱值对象
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Value
public class Email {
    String value;

    public Email(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("邮箱地址不能为空");
        }
        if (!isValidEmail(value)) {
            throw new IllegalArgumentException("邮箱格式不正确: " + value);
        }
        this.value = value.trim().toLowerCase();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
