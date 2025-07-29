package com.pot.user.service.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/3/19 23:30
 * @description: 密码工具类
 */
@Component
@RequiredArgsConstructor
public class PasswordUtils {

    private final PasswordEncoder passwordEncoder;

    // 随机生成加密默认密码
    public String generateDefaultPassword() {
        return passwordEncoder.encode(RandomStringGenerator.generateRandomString(20));
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
}
