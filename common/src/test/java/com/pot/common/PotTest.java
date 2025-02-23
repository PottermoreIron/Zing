package com.pot.common;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * @author: Pot
 * @created: 2025/2/20 23:38
 * @description: 测试
 */
public class PotTest {

    @Test
    void test() {
        // 设置date1为2025-02-20 23:30:00
        Calendar calendar = Calendar.getInstance();
        calendar.set(2025, Calendar.FEBRUARY, 20, 23, 30, 1);
        Date date1 = calendar.getTime();
        // 设置date2为2025-02-20 23:40:00
        calendar.set(2025, Calendar.FEBRUARY, 20, 23, 30, 0);
        Date date2 = calendar.getTime();
        int a = DateUtils.truncatedCompareTo(date1, date2, Calendar.MINUTE);
        System.out.println(a);
    }
}
