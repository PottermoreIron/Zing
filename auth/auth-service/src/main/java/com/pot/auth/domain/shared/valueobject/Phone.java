package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.exception.InvalidPhoneException;
import lombok.Builder;

import java.util.regex.Pattern;

/**
 * 手机号值对象 (Domain Primitive)
 *
 * <p>封装手机号的验证规则和业务行为
 *
 * @author pot
 * @since 2025-12-14
 */
@Builder
public record Phone(String value) {

    private static final Pattern CHINA_MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    /**
     * 紧凑构造器 - 自动验证
     */
    public Phone {
        if (value == null || value.isBlank()) {
            throw new InvalidPhoneException("手机号不能为空");
        }

        String trimmed = value.trim().replaceAll("\\s+", "");

        // 中国大陆手机号或国际手机号
        if (!CHINA_MOBILE_PATTERN.matcher(trimmed).matches()
                && !INTERNATIONAL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidPhoneException("手机号格式不正确: " + value);
        }

        value = trimmed;
    }

    /**
     * 静态工厂方法
     */
    public static Phone of(String value) {
        return new Phone(value);
    }

    /**
     * 是否是中国大陆手机号
     */
    public boolean isChinaMobile() {
        return CHINA_MOBILE_PATTERN.matcher(value).matches();
    }

    /**
     * 获取国际格式（带+86前缀）
     */
    public String toInternationalFormat() {
        if (value.startsWith("+")) {
            return value;
        }
        if (isChinaMobile()) {
            return "+86" + value;
        }
        return value;
    }

    /**
     * 脱敏显示（中间4位隐藏）
     */
    public String toMasked() {
        if (value.length() <= 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}

