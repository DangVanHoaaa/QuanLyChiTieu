package com.example.quanlychitieu.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    public static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    public static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    public static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    public static final SimpleDateFormat DISPLAY_MONTH_FORMAT = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
    public static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.getDefault());

    public static String getTodayIso() {
        return ISO_DATE_FORMAT.format(new Date());
    }

    public static String getCurrentMonth() {
        return MONTH_FORMAT.format(new Date());
    }

    public static String getCurrentYear() {
        return YEAR_FORMAT.format(new Date());
    }

    public static String formatDisplayDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            Date date = ISO_DATE_FORMAT.parse(isoDate);
            if (date != null) {
                return DISPLAY_DATE_FORMAT.format(date);
            }
        } catch (Exception ignored) {
        }
        return isoDate;
    }

    public static String formatDisplayMonth(String monthStr) {
        if (monthStr == null || monthStr.isEmpty()) return "";
        try {
            Date date = MONTH_FORMAT.parse(monthStr);
            if (date != null) {
                return "Tháng " + DISPLAY_MONTH_FORMAT.format(date);
            }
        } catch (Exception ignored) {
        }
        return monthStr;
    }

    public static String[] getStartAndEndOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        String start = ISO_DATE_FORMAT.format(cal.getTime());
        cal.add(Calendar.DAY_OF_WEEK, 6);
        String end = ISO_DATE_FORMAT.format(cal.getTime());
        return new String[]{start, end};
    }

    public static String[] getStartAndEndOfMonth(String monthStr) {
        try {
            Calendar cal = Calendar.getInstance();
            Date date = MONTH_FORMAT.parse(monthStr);
            if (date != null) {
                cal.setTime(date);
            }
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String start = ISO_DATE_FORMAT.format(cal.getTime());
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            String end = ISO_DATE_FORMAT.format(cal.getTime());
            return new String[]{start, end};
        } catch (Exception e) {
            return new String[]{getTodayIso(), getTodayIso()};
        }
    }

    public static String[] getStartAndEndOfYear(String yearStr) {
        return new String[]{yearStr + "-01-01", yearStr + "-12-31"};
    }
}
