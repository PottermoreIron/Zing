package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.exception.InvalidEmailException;

import java.util.regex.Pattern;

/**
 * 邮箱值对象 (Domain Primitive)
 *
 * <p>封装邮箱的验证规则和业务行为
 * <ul>
 *   <li>不可变性：创建后不可修改</li>
 *   <li>自验证：创建时自动验证格式</li>
 *   <li>业务行为：提取域名等</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * 紧凑构造器 - 自动验证
     */
    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidEmailException("邮箱不能为空");
        }

        String trimmed = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidEmailException("邮箱格式不正确: " + value);
        }

        // 重新赋值为规范化的邮箱
        value = trimmed;
    }

    public static Email of(String value) {
        return new Email(value);
    }

    /**
     * 获取邮箱域名
     *
     * @return 域名，如 "gmail.com"
     */
    public String getDomain() {
        int atIndex = value.indexOf('@');
        return value.substring(atIndex + 1);
    }

    /**
     * 获取邮箱用户名部分
     *
     * @return 用户名，如 "user"
     */
    public String getLocalPart() {
        int atIndex = value.indexOf('@');
        return value.substring(0, atIndex);
    }

    /**
     * 判断是否是企业邮箱
     */
    public boolean isCorporateEmail() {
        String domain = getDomain();
        // 常见个人邮箱域名
        return !domain.equals("gmail.com")
                && !domain.equals("qq.com")
                && !domain.equals("163.com")
                && !domain.equals("126.com")
                && !domain.equals("outlook.com")
                && !domain.equals("hotmail.com");
    }
}

