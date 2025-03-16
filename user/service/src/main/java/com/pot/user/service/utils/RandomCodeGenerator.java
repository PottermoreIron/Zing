package com.pot.user.service.utils;

import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;

/**
 * @author: Pot
 * @created: 2025/3/16 22:42
 * @description: 随机码生成器
 */
public class RandomCodeGenerator {
    /**
     * 预定义字符池（线程安全）
     */
    private static final String NUMBERS = "0123456789";
    /**
     * 排除 l/o
     */
    private static final String LOWER_LETTERS = "abcdefghijkmnpqrstuvwxyz";
    /**
     * 排除 I/O
     */
    private static final String UPPER_LETTERS = StringUtils.upperCase(LOWER_LETTERS);
    /**
     * 线程安全随机数
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 预计算混合字符集（按需初始化）
     */
    private static final String ALPHANUMERIC;

    static {
        ALPHANUMERIC = NUMBERS + LOWER_LETTERS + UPPER_LETTERS;
    }

    public static String generateRandomCode(int length, boolean includeLetter) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than 0");
        }
        String charPool = includeLetter ? ALPHANUMERIC : NUMBERS;
        return SECURE_RANDOM.ints(length, 0, charPool.length())
                .map(charPool::charAt)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * 生成随机验证码, 默认不包含字母
     */
    public static String generateRandomCode(int length) {
        return generateRandomCode(length, false);
    }
}
