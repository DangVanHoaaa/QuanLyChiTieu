package com.example.quanlychitieu.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyUtils {

    public static String formatVnd(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(Math.round(amount)) + " VNĐ";
    }

    public static String formatVndWithSign(double amount, String type) {
        String formatted = formatVnd(Math.abs(amount));
        if ("INCOME".equalsIgnoreCase(type)) {
            return "+ " + formatted;
        } else {
            return "- " + formatted;
        }
    }

    public static double parseAmount(String text) {
        if (text == null) return 0;
        try {
            // Loại bỏ toàn bộ ký tự không phải số (loại bỏ dấu chấm hàng nghìn, phẩy, chữ, khoảng trắng)
            String clean = text.replaceAll("[^0-9]", "").trim();
            if (clean.isEmpty()) return 0;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0;
        }
    }
}
