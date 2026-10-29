package com.example.quanlychitieu;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.quanlychitieu.service.BankNotificationParser;
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

    @Test
    public void testBidvAppNotificationExpense() {
        String pkg = "com.vnpay.bidv";
        String title = "Biến động số dư";
        String body = "TK 1234567890: -250,000 VND vao 12:30 28/10/2026. Ref: 123456. So du cuoi: 15,250,000 VND. ND: An trua com ga";

        assertTrue(BankNotificationParser.isBankingNotification(pkg, title, body));

        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(pkg, title, body);
        assertNotNull(parsed);
        assertEquals("BIDV", parsed.bankName);
        assertEquals("EXPENSE", parsed.type);
        assertEquals(250000.0, parsed.amount, 0.001);
        assertEquals("Ăn uống", parsed.categoryName);
        assertTrue(parsed.note.contains("An trua com ga"));
        assertFalse(parsed.note.contains("15,250,000"));
    }

    @Test
    public void testBidvAppNotificationIncome() {
        String pkg = "vn.com.bidv.smartbanking";
        String title = "BIDV SmartBanking";
        String body = "BIDV: TK 1234567890 +15,000,000VND luc 09:00 28/10/2026. So du kha dung: 25,000,000VND. ND: Cong ty ABC tra luong T10";

        assertTrue(BankNotificationParser.isBankingNotification(pkg, title, body));

        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(pkg, title, body);
        assertNotNull(parsed);
        assertEquals("BIDV", parsed.bankName);
        assertEquals("INCOME", parsed.type);
        assertEquals(15000000.0, parsed.amount, 0.001);
        assertEquals("Lương", parsed.categoryName);
        assertTrue(parsed.note.contains("luong"));
    }

    @Test
    public void testBidvSmsGhiNo() {
        // SMS từ tổng đài BIDV
        String pkg = "com.google.android.apps.messaging";
        String title = "BIDV";
        String body = "BIDV: TK 1234567890, Ghi no: 350,000 VND vao 14:00 28/10/2026. So du cuoi: 2,500,000 VND. Dien giai: Mua giay the thao Shopee";

        assertTrue(BankNotificationParser.isBankingNotification(pkg, title, body));

        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(pkg, title, body);
        assertNotNull(parsed);
        assertEquals("BIDV", parsed.bankName);
        assertEquals("EXPENSE", parsed.type);
        assertEquals(350000.0, parsed.amount, 0.001);
        assertEquals("Mua sắm", parsed.categoryName);
        assertTrue(parsed.note.contains("Shopee"));
    }

    @Test
    public void testBidvSmsGhiCo() {
        String pkg = "com.android.mms";
        String title = "BIDV";
        String body = "BIDV: TK 1234567890, Ghi co: 500,000 VND. So du cuoi: 3,000,000 VND. ND: Ban tang tien sinh nhat";

        assertTrue(BankNotificationParser.isBankingNotification(pkg, title, body));

        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(pkg, title, body);
        assertNotNull(parsed);
        assertEquals("BIDV", parsed.bankName);
        assertEquals("INCOME", parsed.type);
        assertEquals(500000.0, parsed.amount, 0.001);
        assertEquals("Tiền được cho", parsed.categoryName);
    }

    @Test
    public void testBidvPhatSinhPattern() {
        String pkg = null;
        String title = "BIDV";
        String body = "TK 123456 tai BIDV: PS: -120.000VND. SD: 1.200.000VND. Mo ta: Do xang xe may";

        assertTrue(BankNotificationParser.isBankingNotification(pkg, title, body));

        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(pkg, title, body);
        assertNotNull(parsed);
        assertEquals("BIDV", parsed.bankName);
        assertEquals("EXPENSE", parsed.type);
        assertEquals(120000.0, parsed.amount, 0.001);
        assertEquals("Đi lại", parsed.categoryName);
        assertTrue(parsed.note.contains("Do xang"));
    }

    @Test
    public void testMbBankNotification() {
        String pkg = "com.mbmobile";
        String title = "MB Bank";
        String body = "TK 0123456789 -150,000VND tai Highlands Coffee. SD: 5,000,000VND";

        assertTrue(BankNotificationParser.isBankingNotification(pkg, title, body));

        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(pkg, title, body);
        assertNotNull(parsed);
        assertEquals("MB Bank", parsed.bankName);
        assertEquals("EXPENSE", parsed.type);
        assertEquals(150000.0, parsed.amount, 0.001);
        assertEquals("Ăn uống", parsed.categoryName);
    }

    @Test
    public void testBidvThanhToanHoaDonEvn() {
        String pkg = "com.bidv.smartbanking";
        String title = "BIDV";
        String body = "BIDV: TK 123456, Thanh toan hoa don EVN 450.000 VND. So du kha dung: 2.100.000 VND";

        assertTrue(BankNotificationParser.isBankingNotification(pkg, title, body));

        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(pkg, title, body);
        assertNotNull(parsed);
        assertEquals("BIDV", parsed.bankName);
        assertEquals("EXPENSE", parsed.type);
        assertEquals(450000.0, parsed.amount, 0.001);
        assertEquals("Hóa đơn", parsed.categoryName);
    }

    @Test
    public void testBidvSoTienGdAnSang() {
        String pkg = "vn.bidv.smartbanking";
        String title = "Biến động số dư BIDV";
        String body = "BIDV: TK 12345678. So tien GD: -80.000 VND. So du cuoi: 420.000 VND. ND: An sang banh mi";

        assertTrue(BankNotificationParser.isBankingNotification(pkg, title, body));

        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(pkg, title, body);
        assertNotNull(parsed);
        assertEquals("BIDV", parsed.bankName);
        assertEquals("EXPENSE", parsed.type);
        assertEquals(80000.0, parsed.amount, 0.001);
        assertEquals("Ăn uống", parsed.categoryName);
        assertTrue(parsed.note.contains("An sang banh mi"));
    }

    @Test
    public void testBidvNhanTienVaoTk() {
        String pkg = "com.vnpay.bidv";
        String title = "BIDV";
        String body = "BIDV: TK 12345678. Nhan 2.000.000 VND tu Nguyen Van B. So du: 5.000.000 VND. ND: Tra tien vay";

        assertTrue(BankNotificationParser.isBankingNotification(pkg, title, body));

        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(pkg, title, body);
        assertNotNull(parsed);
        assertEquals("BIDV", parsed.bankName);
        assertEquals("INCOME", parsed.type);
        assertEquals(2000000.0, parsed.amount, 0.001);
    }
}
