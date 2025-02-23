package com.pot.common.utils;

import java.util.Calendar;
import java.util.Date;

import static org.apache.commons.lang3.time.DateUtils.truncatedCompareTo;

/**
 * @author: Pot
 * @created: 2025/2/20 23:34
 * @description: 日期工具类
 */
public class DateUtils {

    /**
     * @param date1 date1
     * @param date2 date2
     * @param field compare field
     * @return Date
     * @description compare date1 and date2, return the max date
     * @date 16:38 2025/2/22
     **/
    public static Date getMaxDate(Date date1, Date date2, int field) {
        return truncatedCompareTo(date1, date2, field) > 0 ? date1 : date2;
    }

    /**
     * @param date1 date1
     * @param date2 date2
     * @param field compare field
     * @return Date
     * @description compare date1 and date2, return the min date
     * @date 16:38 2025/2/22
     **/
    public static Date getMinDate(Date date1, Date date2, int field) {
        return truncatedCompareTo(date1, date2, field) < 0 ? date1 : date2;
    }

    /**
     * @param date1 date1
     * @param date2 date2
     * @return Date
     * @description compare date1 and date2, return the max date, default compare field is SECOND
     * @date 16:38 2025/2/22
     **/
    public static Date getMaxDate(Date date1, Date date2) {
        return getMaxDate(date1, date2, Calendar.SECOND);
    }

    /**
     * @param date1 date1
     * @param date2 date2
     * @return Date
     * @description compare date1 and date2, return the min date, default compare field is SECOND
     * @date 16:38 2025/2/22
     **/
    public static Date getMinDate(Date date1, Date date2) {
        return getMinDate(date1, date2, Calendar.SECOND);
    }

    /**
     * @param date1 date1
     * @param date2 date2
     * @return boolean
     * @description compare date1 and date2, return true if they are the same day
     * @date 16:38 2025/2/22
     **/
    public static boolean isSameDay(Date date1, Date date2) {
        return truncatedCompareTo(date1, date2, Calendar.DATE) == 0;
    }

    /**
     * @param date1 date1
     * @param date2 date2
     * @return boolean
     * @description compare date1 and date2, return true if date1 is before date2
     * @date 16:38 2025/2/22
     **/
    public static boolean isBefore(Date date1, Date date2) {
        return truncatedCompareTo(date1, date2, Calendar.SECOND) < 0;
    }

    /**
     * @param date1 date1
     * @param date2 date2
     * @return boolean
     * @description compare date1 and date2, return true if date1 is after date2
     * @date 16:38 2025/2/22
     **/
    public static boolean isAfter(Date date1, Date date2) {
        return truncatedCompareTo(date1, date2, Calendar.SECOND) > 0;
    }

}
