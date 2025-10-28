package com.pot.zing.framework.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/8/16 22:13
 * @description: 密码工具类
 */
@Component
public class PasswordUtils {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    // 随机生成加密默认密码
    public static String generateDefaultPassword() {
        return PASSWORD_ENCODER.encode(RandomUtils.generateRandomString(20));
    }

    public static String encodePassword(String password) {
        return PASSWORD_ENCODER.encode(password);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }
}
