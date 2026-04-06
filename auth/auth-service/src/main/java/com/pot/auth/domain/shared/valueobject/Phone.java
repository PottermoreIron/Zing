package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.exception.InvalidPhoneException;
import lombok.Builder;

import java.util.regex.Pattern;

@Builder
public record Phone(String value) {

    private static final Pattern CHINA_MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

        public Phone {
        if (value == null || value.isBlank()) {
            throw new InvalidPhoneException("手机号不能为空");
        }

        String trimmed = value.trim().replaceAll("\\s+", "");

        if (!CHINA_MOBILE_PATTERN.matcher(trimmed).matches()
                && !INTERNATIONAL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidPhoneException("手机号格式不正确: " + value);
        }

        value = trimmed;
    }

        public static Phone of(String value) {
        return new Phone(value);
    }

        public boolean isChinaMobile() {
        return CHINA_MOBILE_PATTERN.matcher(value).matches();
    }

        public String toInternationalFormat() {
        if (value.startsWith("+")) {
            return value;
        }
        if (isChinaMobile()) {
            return "+86" + value;
        }
        return value;
    }

        public String toMasked() {
        if (value.length() <= 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}

