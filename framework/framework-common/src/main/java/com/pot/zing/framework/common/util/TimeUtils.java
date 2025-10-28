package com.pot.zing.framework.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author: Pot
 * @created: 2025/10/19 22:43
 * @description: 时间工具类
 */
public class TimeUtils {
    /**
     * 将时间戳转换为LocalDateTime
     *
     * @param timestamp 时间戳
     * @return LocalDateTime对象
     */
    public static LocalDateTime toLocalDateTime(long timestamp, ZoneId zoneId) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                zoneId
        );
    }

    /**
     * 将LocalDateTime转换为时间戳
     *
     * @param localDateTime LocalDateTime对象
     * @return 时间戳
     */
    public static Long toTimestamp(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 获取当前时间戳
     *
     * @return 当前时间戳
     */
    public static Long currentTimestamp() {
        return Instant.now().toEpochMilli();
    }

}
