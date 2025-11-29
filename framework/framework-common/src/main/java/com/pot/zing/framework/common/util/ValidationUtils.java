package com.pot.zing.framework.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * @author: Pot
 * @created: 2025/2/22 16:48
 * @description: 验证工具类
 */
public class ValidationUtils {
    public final static String PHONE_REGEX = "(?:0|86|\\+86)?1[3-9]\\d{9}$";
    public final static String NICKNAME_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9_-]{1,30}$";
    public final static String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,16}$";
    public final static String EMAIL_REGEX = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
    public final static String IP_V4_REGEX = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
    public final static String VERIFICATION_CODE_REGEX = "^[0-9]{6}$";

    public static boolean isValidPhone(String phone) {
        return isValidPhone(phone, PHONE_REGEX);
    }

    public static boolean isValidPhone(String phone, String pattern) {
        return isMatch(phone, pattern);
    }

    public static boolean isValidEmail(String email) {
        return isValidEmail(email, EMAIL_REGEX);
    }

    public static boolean isValidEmail(String email, String pattern) {
        return isMatch(email, pattern);
    }

    public static boolean isValidNickname(String nickname) {
        return isValidNickname(nickname, NICKNAME_REGEX);
    }

    public static boolean isValidNickname(String nickname, String pattern) {
        return isMatch(nickname, pattern);
    }

    public static boolean isValidPassword(String password) {
        return isValidPassword(password, PASSWORD_REGEX);
    }

    public static boolean isValidPassword(String password, String pattern) {
        return isMatch(password, pattern);
    }

    public static boolean isValidIpV4(String ip) {
        return isValidIpV4(ip, IP_V4_REGEX);
    }

    public static boolean isValidIpV4(String ip, String pattern) {
        return isMatch(ip, pattern);
    }

    public static boolean isValidVerificationCode(String code) {
        return isValidVerificationCode(code, VERIFICATION_CODE_REGEX);
    }

    public static boolean isValidVerificationCode(String code, String pattern) {
        return isMatch(code, pattern);
    }

    public static void main(String[] args) {
        System.out.println(isValidPassword("Aa1!Bc2@"));
    }

    public static boolean isMatch(String str, String pattern) {
        if (str == null || pattern == null) {
            return false;
        }
        return Pattern.compile(pattern).matcher(str).matches();
    }

    public static boolean isValidIdentifier(String identifier) {
        return isValidIdentifier(identifier, null);
    }

    public static boolean isValidIdentifier(String identifier, String pattern) {
        if (StringUtils.isBlank(pattern)) {
            boolean isValidNickname = isValidNickname(identifier);
            boolean isValidEmail = isValidEmail(identifier);
            boolean isValidPhone = isValidPhone(identifier);
            return isValidNickname ^ isValidEmail ^ isValidPhone;
        } else {
            return isMatch(identifier, pattern);
        }
    }
}
