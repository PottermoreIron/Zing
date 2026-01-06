package com.pot.member.service.domain.model.member;

import lombok.Value;

/**
 * 手机号值对象
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Value
public class PhoneNumber {
    String value;

    public PhoneNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        String cleanedPhone = value.trim().replaceAll("\\s+", "");
        if (!isValidPhone(cleanedPhone)) {
            throw new IllegalArgumentException("手机号格式不正确: " + value);
        }
        this.value = cleanedPhone;
    }

    private boolean isValidPhone(String phone) {
        // 支持国际手机号格式: +86 13800138000 或 13800138000
        String phoneRegex = "^(\\+?86)?1[3-9]\\d{9}$";
        return phone.matches(phoneRegex);
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }
}
