package com.pot.member.service.domain.model.member;

import lombok.Value;

@Value
public class Nickname {

    String value;

    public Nickname(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Nickname must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 50) {
            throw new IllegalArgumentException("Nickname must be between 2 and 50 characters");
        }
        if (!isValidNickname(trimmed)) {
            throw new IllegalArgumentException("Nickname may only contain CJK characters, letters, digits, underscores, and hyphens");
        }
        this.value = trimmed;
    }

    private static boolean isValidNickname(String nickname) {
        return nickname.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_-]+$");
    }

    public static Nickname of(String value) {
        return new Nickname(value);
    }
}
