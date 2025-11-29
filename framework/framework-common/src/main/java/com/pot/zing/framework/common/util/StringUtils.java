package com.pot.zing.framework.common.util;

/**
 * @author: Pot
 * @created: 2025/2/22 16:46
 * @description: 字符串工具类
 */
public class StringUtils {

    // 字符串判null+empty方法, 使用apache commons-lang3实现
    public static boolean isEmpty(String str) {
        return org.apache.commons.lang3.StringUtils.isEmpty(str);
    }
}
