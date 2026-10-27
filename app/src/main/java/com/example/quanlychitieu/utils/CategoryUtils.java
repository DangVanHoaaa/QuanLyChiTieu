package com.example.quanlychitieu.utils;

import android.content.Context;

import com.example.quanlychitieu.R;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class CategoryUtils {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static String stripAccents(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    public static int getCategoryIconRes(Context context, String nameOrIcon) {
        if (nameOrIcon == null || nameOrIcon.trim().isEmpty()) {
            return R.drawable.ic_cat_expense_other;
        }

        String raw = nameOrIcon.trim().toLowerCase(Locale.ROOT);
        String norm = stripAccents(raw);

        // 1. Tiết kiệm / Heo đất
        if (norm.contains("tiet kiem") || raw.contains("savings") || raw.contains("piggy")) {
            return R.drawable.ic_piggy_bank;
        }

        // 2. Ăn uống / Nhà hàng / Cafe / Đồ uống
        if (norm.contains("an uong") || norm.contains("food") || norm.contains("cafe") || norm.contains("ca phe")
                || norm.contains("an sang") || norm.contains("an trua") || norm.contains("an toi")
                || norm.contains("com") || norm.contains("do an")) {
            return R.drawable.ic_cat_food;
        }

        // 3. Lương
        if (norm.contains("luong") || norm.contains("salary")) {
            return R.drawable.ic_cat_salary;
        }

        // 4. Thưởng
        if (norm.contains("thuong") || norm.contains("bonus") || norm.contains("hoa hong")) {
            return R.drawable.ic_cat_bonus;
        }

        // 5. Tiền được cho / Quà tặng / Lì xì
        if (norm.contains("cho") || norm.contains("tang") || norm.contains("gift") || norm.contains("li xi") || norm.contains("mung tuoi")) {
            return R.drawable.ic_cat_gift;
        }

        // 6. Đi lại / Giao thông / Xăng xe / Grab / Taxi
        if (norm.contains("di lai") || norm.contains("xang") || norm.contains("xe") || norm.contains("transport")
                || norm.contains("grab") || norm.contains("be") || norm.contains("taxi")) {
            return R.drawable.ic_cat_transport;
        }

        // 7. Học tập / Sách vở / Học phí
        if (norm.contains("hoc") || norm.contains("education") || norm.contains("sach") || norm.contains("khoa hoc")) {
            return R.drawable.ic_cat_education;
        }

        // 8. Mua sắm / Shopee / Quần áo / Siêu thị
        if (norm.contains("mua sam") || norm.contains("shopping") || norm.contains("shopee") || norm.contains("tiki")
                || norm.contains("lazada") || norm.contains("quan ao") || norm.contains("sieu thi") || norm.contains("cho")) {
            return R.drawable.ic_cat_shopping;
        }

        // 9. Hóa đơn / Điện nước / Internet / Cước phí
        if (norm.contains("hoa don") || norm.contains("bill") || norm.contains("dien") || norm.contains("nuoc")
                || norm.contains("internet") || norm.contains("wifi") || norm.contains("cuoc")) {
            return R.drawable.ic_cat_bill;
        }

        // 10. Giải trí / Xem phim / Game / Du lịch
        if (norm.contains("giai tri") || norm.contains("entertainment") || norm.contains("game")
                || norm.contains("phim") || norm.contains("du lich") || norm.contains("karaoke")) {
            return R.drawable.ic_cat_entertainment;
        }

        // 11. Sức khỏe / Y tế / Thuốc / Bệnh viện
        if (norm.contains("suc khoe") || norm.contains("health") || norm.contains("y te")
                || norm.contains("thuoc") || norm.contains("benh") || norm.contains("kham")) {
            return R.drawable.ic_cat_health;
        }

        // 12. Nhà ở / Thuê nhà / Phòng trọ
        if (norm.contains("nha") || norm.contains("home") || norm.contains("thue nha")
                || norm.contains("phong tro") || norm.contains("chung cu")) {
            return R.drawable.ic_cat_home;
        }

        // 13. Thu nhập khác
        if (norm.contains("thu nhap") || norm.contains("income")) {
            return R.drawable.ic_cat_income_other;
        }

        // Khớp theo tên file icon trực tiếp nếu có
        switch (raw) {
            case "ic_cat_salary": return R.drawable.ic_cat_salary;
            case "ic_cat_bonus": return R.drawable.ic_cat_bonus;
            case "ic_cat_gift": return R.drawable.ic_cat_gift;
            case "ic_cat_income_other": return R.drawable.ic_cat_income_other;
            case "ic_cat_food": return R.drawable.ic_cat_food;
            case "ic_cat_transport": return R.drawable.ic_cat_transport;
            case "ic_cat_education": return R.drawable.ic_cat_education;
            case "ic_cat_shopping": return R.drawable.ic_cat_shopping;
            case "ic_cat_bill": return R.drawable.ic_cat_bill;
            case "ic_cat_entertainment": return R.drawable.ic_cat_entertainment;
            case "ic_cat_health": return R.drawable.ic_cat_health;
            case "ic_cat_home": return R.drawable.ic_cat_home;
            case "ic_nav_savings":
            case "ic_piggy_bank": return R.drawable.ic_piggy_bank;
            default: return R.drawable.ic_cat_expense_other;
        }
    }
}
