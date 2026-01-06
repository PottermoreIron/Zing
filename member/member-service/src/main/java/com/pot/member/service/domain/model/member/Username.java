package com.pot.member.service.domain.model.member;

import lombok.Value;

/**
 * 用户名值对象
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Value
public class Username {
    String value;

    public Username(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 50) {
            throw new IllegalArgumentException("用户名长度必须在2-50个字符之间");
        }
        if (!isValidUsername(trimmed)) {
            throw new IllegalArgumentException("用户名只能包含中文、英文、数字、下划线和横线");
        }
        this.value = trimmed;
    }

    private boolean isValidUsername(String username) {
        String usernameRegex = "^[\\u4e00-\\u9fa5a-zA-Z0-9_-]+$";
        return username.matches(usernameRegex);
    }

    public static Username of(String value) {
        return new Username(value);
    }
}
