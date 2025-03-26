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

    public static boolean isPhone(String phone) {
        return isPhone(phone, PHONE_REGEX);
    }

    public static boolean isPhone(String phone, String pattern) {
        return isMatch(phone, pattern);
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
