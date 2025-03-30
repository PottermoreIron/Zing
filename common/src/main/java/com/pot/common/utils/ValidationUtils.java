package com.pot.common.utils;

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

    public static boolean isPhone(String phone) {
        return isPhone(phone, PHONE_REGEX);
    }

    public static boolean isPhone(String phone, String pattern) {
        return isMatch(phone, pattern);
    }

    public static boolean isEmail(String email) {
        return isEmail(email, EMAIL_REGEX);
    }

    public static boolean isEmail(String email, String pattern) {
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


    public static void main(String[] args) {
        System.out.println(isValidPassword("Aa1!Bc2@"));
    }

    public static boolean isMatch(String str, String pattern) {
        if (str == null || pattern == null) {
            return false;
        }
        return Pattern.compile(pattern).matcher(str).matches();
    }
}
