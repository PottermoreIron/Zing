package com.pot.zing.framework.common.util;

import java.util.Calendar;
import java.util.Date;

import static org.apache.commons.lang3.time.DateUtils.truncatedCompareTo;

/**
 * Date comparison helpers.
 */
public class DateUtils {

    /**
     * Returns the later date using the supplied truncation field.
     */
    public static Date getMaxDate(Date date1, Date date2, int field) {
        return truncatedCompareTo(date1, date2, field) > 0 ? date1 : date2;
    }

    /**
     * Returns the earlier date using the supplied truncation field.
     */
    public static Date getMinDate(Date date1, Date date2, int field) {
        return truncatedCompareTo(date1, date2, field) < 0 ? date1 : date2;
    }

    /**
     * Returns the later date using second precision.
     */
    public static Date getMaxDate(Date date1, Date date2) {
        return getMaxDate(date1, date2, Calendar.SECOND);
    }

    /**
     * Returns the earlier date using second precision.
     */
    public static Date getMinDate(Date date1, Date date2) {
        return getMinDate(date1, date2, Calendar.SECOND);
    }

    /**
     * Returns whether the dates fall on the same day.
     */
    public static boolean isSameDay(Date date1, Date date2) {
        return truncatedCompareTo(date1, date2, Calendar.DATE) == 0;
    }

    /**
     * Returns whether the first date is before the second.
     */
    public static boolean isBefore(Date date1, Date date2) {
        return truncatedCompareTo(date1, date2, Calendar.SECOND) < 0;
    }

    /**
     * Returns whether the first date is after the second.
     */
    public static boolean isAfter(Date date1, Date date2) {
        return truncatedCompareTo(date1, date2, Calendar.SECOND) > 0;
    }

}
