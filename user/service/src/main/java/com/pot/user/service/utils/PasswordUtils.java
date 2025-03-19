package com.pot.user.service.utils;

/**
 * @author: Pot
 * @created: 2025/3/19 23:30
 * @description: 密码工具类
 */
public class PasswordUtils {

    // 随机生成加密默认密码
    public static String generateDefaultPassword() {
        return RandomStringGenerator.generateRandomString(20);
    }
}
