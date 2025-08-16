package com.pot.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/8/16 22:13
 * @description: 密码工具类
 */
@Component
@RequiredArgsConstructor
public class PasswordUtils {

    private final PasswordEncoder passwordEncoder;

    // 随机生成加密默认密码
    public String generateDefaultPassword() {
        return passwordEncoder.encode(RandomUtils.generateRandomString(20));
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
}
