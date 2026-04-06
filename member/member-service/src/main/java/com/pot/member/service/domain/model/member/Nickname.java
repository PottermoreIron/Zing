package com.pot.member.service.domain.model.member;

import lombok.Value;

@Value
public class Nickname {

    String value;

    public Nickname(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 50) {
            throw new IllegalArgumentException("昵称长度必须在2-50个字符之间");
        }
        if (!isValidNickname(trimmed)) {
            throw new IllegalArgumentException("昵称只能包含中文、英文、数字、下划线和横线");
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
