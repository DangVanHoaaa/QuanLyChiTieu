package com.example.quanlychitieu;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;

public class AppLogicUnitTest {

    @Test
    public void testCurrencyFormatting() {
        assertEquals("1.500.000 VNĐ", CurrencyUtils.formatVnd(1500000));
        assertEquals("0 VNĐ", CurrencyUtils.formatVnd(0));
        assertEquals("+ 5.000.000 VNĐ", CurrencyUtils.formatVndWithSign(5000000, "INCOME"));
        assertEquals("- 250.000 VNĐ", CurrencyUtils.formatVndWithSign(2500000 / 10, "EXPENSE"));
    }

    @Test
    public void testCurrencyParsing() {
        assertEquals(1500000.0, CurrencyUtils.parseAmount("1500000"), 0.001);
        assertEquals(1500000.0, CurrencyUtils.parseAmount("1,500,000"), 0.001);
        assertEquals(0.0, CurrencyUtils.parseAmount("abc"), 0.001);
        assertEquals(0.0, CurrencyUtils.parseAmount(""), 0.001);
    }

    @Test
    public void testDateUtilsFormatting() {
        assertEquals("26/10/2026", DateUtils.formatDisplayDate("2026-10-26"));
        assertEquals("Tháng 10/2026", DateUtils.formatDisplayMonth("2026-10"));
    }

    @Test
    public void testBalanceCalculation() {
        double totalIncome = 15000000;
        double totalExpense = 4500000;
        double balance = totalIncome - totalExpense;
        assertEquals(10500000.0, balance, 0.001);
    }

    @Test
    public void testBudgetCalculations() {
        double budget = 5000000;
        double spentSafe = 3000000;
        double remainingSafe = budget - spentSafe;
        int percentSafe = (int) Math.round((spentSafe / budget) * 100);
        assertEquals(2000000.0, remainingSafe, 0.001);
        assertEquals(60, percentSafe);
        assertFalse(percentSafe >= 80);
        assertFalse(spentSafe > budget);

        // Ngưỡng cảnh báo 80%
        double spentWarning = 4200000;
        int percentWarning = (int) Math.round((spentWarning / budget) * 100);
        assertEquals(84, percentWarning);
        assertTrue(percentWarning >= 80);
        assertFalse(spentWarning > budget);

        // Vượt ngân sách
        double spentOver = 5500000;
        assertTrue(spentOver > budget);
    }

    @Test
    public void testSavingsGoalCalculations() {
        double target = 5000000;
        double current = 2000000;
        int progress = (int) Math.round((current / target) * 100);
        assertEquals(40, progress);
        assertFalse(current >= target);

        double deposit = 3000000;
        double newCurrent = current + deposit;
        int newProgress = (int) Math.round((newCurrent / target) * 100);
        assertEquals(100, newProgress);
        assertTrue(newCurrent >= target);
    }

    @Test
    public void testAiNlpAmountExtraction() {
        // Kiểm tra nhận diện 45 nghìn -> 45000
        String text1 = "hôm nay tôi ăn trưa hết 45 nghìn";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|nghìn|ngàn|k|đ|vnd|dong)?", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m1 = p.matcher(text1);
        assertTrue(m1.find());
        assertEquals("45", m1.group(1));
        assertEquals("nghìn", m1.group(2));

        // Kiểm tra nhận diện 1.5 triệu -> 1500000
        String text2 = "vừa nhận thưởng 1.5 triệu";
        java.util.regex.Matcher m2 = p.matcher(text2);
        assertTrue(m2.find());
        assertEquals("1.5", m2.group(1));
        assertEquals("triệu", m2.group(2));
    }
}
