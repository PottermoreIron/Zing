package com.pot.zing.framework.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Time conversion helpers.
 */
public class TimeUtils {

    /**
     * Converts an epoch-millis timestamp to a local date-time.
     */
    public static LocalDateTime toLocalDateTime(long timestamp, ZoneId zoneId) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                zoneId);
    }

    /**
     * Converts a local date-time to epoch millis.
     */
    public static Long toTimestamp(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Returns the current epoch-millis timestamp.
     */
    public static Long currentTimestamp() {
        return Instant.now().toEpochMilli();
    }

}
