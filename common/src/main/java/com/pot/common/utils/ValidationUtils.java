package com.pot.common.utils;

import java.util.regex.Pattern;

/**
 * @author: Pot
 * @created: 2025/2/22 16:48
 * @description: 验证工具类
 */
public class ValidationUtils {
    private final static String PHONE_REGEX = "(?:0|86|\\+86)?1[3-9]\\d{9}$";
    public final static Pattern PHONE = Pattern.compile(PHONE_REGEX);

    public static boolean isPhone(String phone) {
        return isMatch(phone, PHONE);
    }

    public static void main(String[] args) {
        System.out.println(isPhone(null));
    }

    public static boolean isMatch(String str, Pattern pattern) {
        if (str == null || pattern == null) {
            return false;
        }
        return pattern.matcher(str).matches();
    }
}
