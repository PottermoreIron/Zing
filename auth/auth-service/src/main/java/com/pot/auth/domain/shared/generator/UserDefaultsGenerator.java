package com.pot.auth.domain.shared.generator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户默认信息生成器
 *
 * <p>
 * 用于一键注册时生成默认的用户名、密码、头像等信息
 *
 * <p>
 * 应用场景：
 * <ul>
 * <li>手机号验证码一键注册 - 生成用户名和密码</li>
 * <li>邮箱验证码一键注册 - 生成用户名和密码</li>
 * <li>OAuth2一键注册 - 生成密码</li>
 * <li>所有注册方式 - 提供默认头像</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDefaultsGenerator {

    /**
     * 默认头像URL（可通过配置文件覆盖）
     */
    private static final String DEFAULT_AVATAR_URL = "https://cdn.example.com/avatars/default.png";

    /**
     * 用户名前缀
     */
    private static final String USERNAME_PREFIX = "user_";

    /**
     * 特殊字符集合
     */
    private static final String SPECIAL_CHARS = "!@#$%^&*";

    /**
     * 字母数字字符集合
     */
    private static final String ALPHANUMERIC_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 字母字符集合
     */
    private static final String ALPHABETIC_CHARS = "abcdefghijklmnopqrstuvwxyz";

    /**
     * 数字字符集合
     */
    private static final String NUMERIC_CHARS = "0123456789";

    /**
     * 随机数生成器
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 基于手机号生成用户名
     *
     * <p>
     * 格式: user_{timestamp}_{random4}
     * <p>
     * 示例: user_1735123456789_a1b2
     *
     * @param phone 手机号
     * @return 生成的用户名
     */
    public String generateUsernameFromPhone(String phone) {
        long timestamp = System.currentTimeMillis();
        String random = randomAlphanumeric(4).toLowerCase();
        String username = USERNAME_PREFIX + timestamp + "_" + random;

        log.debug("[用户默认值生成] 基于手机号生成用户名: phone={}, username={}", phone, username);
        return username;
    }

    /**
     * 基于邮箱生成用户名
     *
     * <p>
     * 格式: {emailPrefix}_{random4}
     * <p>
     * 示例: john_a1b2
     *
     * @param email 邮箱地址
     * @return 生成的用户名
     */
    public String generateUsernameFromEmail(String email) {
        String prefix = email.substring(0, email.indexOf("@"));
        String random = randomAlphanumeric(4).toLowerCase();
        String username = prefix + "_" + random;

        log.debug("[用户默认值生成] 基于邮箱生成用户名: email={}, username={}", email, username);
        return username;
    }

    /**
     * 生成通用用户名
     *
     * <p>
     * 格式: user_{timestamp}_{random6}
     *
     * @return 生成的用户名
     */
    public String generateUsername() {
        long timestamp = System.currentTimeMillis();
        String random = randomAlphanumeric(6).toLowerCase();
        String username = USERNAME_PREFIX + timestamp + "_" + random;

        log.debug("[用户默认值生成] 生成通用用户名: username={}", username);
        return username;
    }

    /**
     * 生成随机密码
     *
     * <p>
     * 密码规则：
     * <ul>
     * <li>长度: 12位</li>
     * <li>包含大写字母: 2位</li>
     * <li>包含小写字母: 6位</li>
     * <li>包含数字: 3位</li>
     * <li>包含特殊字符: 1位 (!@#$%^&*)</li>
     * </ul>
     *
     * <p>
     * 示例: Abc123def!45
     *
     * @return 生成的随机密码
     */
    public String generateRandomPassword() {
        // 生成各类字符
        String uppercase = randomAlphabetic(2).toUpperCase();
        String lowercase = randomAlphabetic(6).toLowerCase();
        String digits = randomNumeric(3);
        String special = String.valueOf(SPECIAL_CHARS.charAt(
                ThreadLocalRandom.current().nextInt(SPECIAL_CHARS.length())));

        // 组合并打乱顺序
        String combined = uppercase + lowercase + digits + special;
        String password = shuffleString(combined);

        log.debug("[用户默认值生成] 生成随机密码: length={}", password.length());
        return password;
    }

    /**
     * 获取默认头像URL
     *
     * <p>
     * 可通过配置文件 auth.defaults.avatar-url 覆盖
     *
     * @return 默认头像URL
     */
    public String getDefaultAvatarUrl() {
        return DEFAULT_AVATAR_URL;
    }

    /**
     * 打乱字符串顺序
     *
     * @param input 输入字符串
     * @return 打乱后的字符串
     */
    private String shuffleString(String input) {
        List<Character> characters = new ArrayList<>();
        for (char c : input.toCharArray()) {
            characters.add(c);
        }
        Collections.shuffle(characters);

        StringBuilder result = new StringBuilder();
        for (char c : characters) {
            result.append(c);
        }
        return result.toString();
    }

    /**
     * 生成随机字母数字字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    private String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC_CHARS.charAt(RANDOM.nextInt(ALPHANUMERIC_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成随机字母字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    private String randomAlphabetic(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABETIC_CHARS.charAt(RANDOM.nextInt(ALPHABETIC_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成随机数字字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    private String randomNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMERIC_CHARS.charAt(RANDOM.nextInt(NUMERIC_CHARS.length())));
        }
        return sb.toString();
    }
}
