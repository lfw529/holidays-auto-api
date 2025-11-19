package com.lfw.holiday.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类
 */
public class DateUtils {
    private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LocalDate parse(String dateStr) {
        return LocalDate.parse(dateStr, YYYY_MM_DD);
    }

    public static String toMmDd(String fullDate) {
        return fullDate.substring(5); // "2024-01-01" -> "01-01"
    }
}