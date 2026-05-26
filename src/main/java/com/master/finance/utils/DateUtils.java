package com.master.finance.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }

    public static String formatShortDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(SHORT_DATE_FORMATTER) : "";
    }

    public static String formatMonthYear(YearMonth yearMonth) {
        return yearMonth != null ? yearMonth.format(MONTH_FORMATTER) : "";
    }

    public static LocalDateTime startOfMonth(int year, int month) {
        return LocalDateTime.of(year, month, 1, 0, 0);
    }

    public static LocalDateTime endOfMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return ym.atEndOfMonth().atTime(23, 59, 59);
    }

    public static LocalDateTime startOfDay(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 0, 0);
    }

    public static LocalDateTime endOfDay(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 23, 59, 59);
    }

    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    public static boolean isOverdue(LocalDate dueDate) {
        return dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    public static boolean isInCurrentMonth(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        YearMonth current = YearMonth.now();
        return YearMonth.from(dateTime).equals(current);
    }
}
