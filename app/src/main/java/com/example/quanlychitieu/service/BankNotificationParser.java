package com.example.quanlychitieu.service;

import com.example.quanlychitieu.ai.AiAssistantManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankNotificationParser {

    public static class ParsedBankTransaction {
        public String bankName;
        public String type; // "INCOME" hoặc "EXPENSE"
        public double amount;
        public String note;
        public String categoryName;
        public String rawContent;

        @Override
        public String toString() {
            return "ParsedBankTransaction{" +
                    "bankName='" + bankName + '\'' +
                    ", type='" + type + '\'' +
                    ", amount=" + amount +
                    ", note='" + note + '\'' +
                    ", categoryName='" + categoryName + '\'' +
                    '}';
        }
    }

    // Danh sách các package ngân hàng và ví điện tử phổ biến tại Việt Nam
    private static final Map<String, String> BANK_PACKAGES = new HashMap<>();
    static {
        BANK_PACKAGES.put("com.vcb.digibank", "Vietcombank");
        BANK_PACKAGES.put("com.VCB", "Vietcombank");
        BANK_PACKAGES.put("com.mbmobile", "MB Bank");
        BANK_PACKAGES.put("com.mbmobile.retail", "MB Bank");
        BANK_PACKAGES.put("vn.com.techcombank.bb.app", "Techcombank");
        BANK_PACKAGES.put("com.techcombank.mobile", "Techcombank");
        BANK_PACKAGES.put("com.vpb.neo", "VPBank");
        BANK_PACKAGES.put("com.vpb.mc", "VPBank");
        BANK_PACKAGES.put("mobile.acb.com.vn", "ACB");
        BANK_PACKAGES.put("com.vnpay.bidv", "BIDV");
        BANK_PACKAGES.put("vn.com.bidv.smartbanking", "BIDV");
        BANK_PACKAGES.put("com.bidv.smartbanking", "BIDV");
        BANK_PACKAGES.put("com.bidv", "BIDV");
        BANK_PACKAGES.put("vn.bidv.smartbanking", "BIDV");
        BANK_PACKAGES.put("com.vnpay.agribank", "Agribank");
        BANK_PACKAGES.put("com.tpb.mb.gprsandroid", "TPBank");
        BANK_PACKAGES.put("com.mservice.momopay", "MoMo");
        BANK_PACKAGES.put("vn.com.vng.zalopay", "ZaloPay");
        BANK_PACKAGES.put("com.bplus.vtpay", "Viettel Money");
        BANK_PACKAGES.put("vn.com.cake.app", "Cake by VPBank");
        BANK_PACKAGES.put("com.timo.vn", "Timo");
        BANK_PACKAGES.put("com.vnpay.sacombank", "Sacombank");
        BANK_PACKAGES.put("com.vib.myvib2", "VIB");
        BANK_PACKAGES.put("com.vietinbank.ipay", "VietinBank");
        BANK_PACKAGES.put("com.scb.mb", "SCB");
        BANK_PACKAGES.put("com.shb.mobile", "SHB");
        BANK_PACKAGES.put("com.hdbank.mbank", "HDBank");
        BANK_PACKAGES.put("com.ocb.omni.app", "OCB");
        BANK_PACKAGES.put("com.seabank.mb", "SeABank");
        BANK_PACKAGES.put("vn.com.msb.smartBanking", "MSB");
        BANK_PACKAGES.put("com.shinhan.smartbanking", "Shinhan Bank");
        BANK_PACKAGES.put("com.lpb.lienviet24h", "LPBank");
    }

    /**
     * Kiểm tra xem thông báo có xuất phát từ app ngân hàng hoặc có nội dung biến động số dư hay không
     */
    public static boolean isBankingNotification(String packageName, String title, String text) {
        if (packageName != null) {
            String lowerPkg = packageName.toLowerCase(Locale.ROOT);
            // MB Bank - Ngân hàng Quân đội
            if (lowerPkg.contains("mbmobile") || lowerPkg.contains("mbbank") || lowerPkg.contains("mb.android")) {
                return true;
            }
            // BIDV
            if (lowerPkg.contains("bidv") || lowerPkg.contains("smartbanking")) {
                return true;
            }
            for (String key : BANK_PACKAGES.keySet()) {
                if (lowerPkg.contains(key.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }

        String fullText = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase(Locale.ROOT);
        String norm = AiAssistantManager.stripAccents(fullText);

        // MB Bank keywords
        if (norm.contains("mbbank") || norm.contains("mb bank") || norm.contains("mb-bank") || norm.contains("ngan hang quan doi")) {
            return true;
        }

        // BIDV keywords
        if (norm.contains("bidv") || norm.contains("smartbanking") || norm.contains("smart banking") || norm.contains("dau tu va phat trien")) {
            return true;
        }

        // Kiểm tra xem có phải app tin nhắn SMS không
        boolean isSmsApp = packageName != null && (
                packageName.contains("messaging")
                || packageName.contains("mms")
                || packageName.contains("message")
        );

        // Kiểm tra từ khóa tên ngân hàng hoặc ví điện tử
        boolean hasBankKeyword = norm.contains("vietcombank") || norm.contains("vcb")
                || norm.contains("mbbank") || norm.contains("mb bank")
                || norm.contains("techcombank") || norm.contains("vpbank")
                || norm.contains("bidv") || norm.contains("agribank")
                || norm.contains("tpbank") || norm.contains("acb")
                || norm.contains("momo") || norm.contains("zalopay")
                || norm.contains("viettel money") || norm.contains("vietinbank")
                || norm.contains("sacombank") || norm.contains("vib")
                || norm.contains("shb") || norm.contains("hdbank")
                || norm.contains("msb") || norm.contains("seabank")
                || norm.contains("ocb") || norm.contains("timo") || norm.contains("cake")
                || norm.contains("ngan hang") || norm.contains("bank");

        // Kiểm tra từ khóa giao dịch tài chính / biến động số dư
        boolean hasBalanceKeyword = norm.contains("bien dong so du")
                || norm.contains("giao dich") || norm.contains("thanh toan")
                || norm.contains("chuyen khoan") || norm.contains("chuyen tien")
                || norm.contains("tai khoan") || norm.contains("tk:") || norm.contains("tk ")
                || norm.contains("stk") || norm.contains("so du") || norm.contains("sodu")
                || norm.contains("so tien") || norm.contains("sotien")
                || norm.contains("gd:") || norm.contains("gd ")
                || norm.contains("ps:") || norm.contains("ps ") || norm.contains("phat sinh")
                || norm.contains("ghi no") || norm.contains("ghi co")
                || norm.contains("dien giai") || norm.contains("so du cuoi");

        // Kiểm tra mẫu định dạng số tiền bằng Regex DOTALL
        Pattern amountPattern = Pattern.compile("(?s)(?:[+-]\\s*[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{1,3}(?:[.,][0-9]{3})+\\s*(?:vnd|vnđ|vcb|mb|d|đ|dong))", Pattern.CASE_INSENSITIVE);
        boolean hasAmountPattern = amountPattern.matcher(fullText).find();

        if (hasBankKeyword && (hasBalanceKeyword || hasAmountPattern)) {
            return true;
        }

        if (isSmsApp && hasBalanceKeyword && hasAmountPattern) {
            return true;
        }

        return hasBalanceKeyword && hasAmountPattern;
    }

    /**
     * Bóc tách thông tin giao dịch từ thông báo ngân hàng
     */
    public static ParsedBankTransaction parse(String packageName, String title, String text) {
        if (title == null) title = "";
        if (text == null) text = "";
        String fullContent = title + "\n" + text;
        String lowerContent = fullContent.toLowerCase(Locale.ROOT);
        String normContent = AiAssistantManager.stripAccents(lowerContent);

        // 1. Xác định tên ngân hàng
        String bankName = "Ngân hàng";
        if (packageName != null) {
            String lowerPkg = packageName.toLowerCase(Locale.ROOT);
            if (lowerPkg.contains("bidv") || lowerPkg.contains("smartbanking")) {
                bankName = "BIDV";
            } else if (lowerPkg.contains("mbmobile") || lowerPkg.contains("mbbank") || lowerPkg.contains("mb.android")) {
                bankName = "MB Bank";
            } else {
                for (Map.Entry<String, String> entry : BANK_PACKAGES.entrySet()) {
                    if (lowerPkg.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                        bankName = entry.getValue();
                        break;
                    }
                }
            }
        }
        if ("Ngân hàng".equals(bankName)) {
            if (normContent.contains("bidv") || normContent.contains("smartbanking") || normContent.contains("smart banking") || normContent.contains("dau tu va phat trien")) bankName = "BIDV";
            else if (normContent.contains("mbbank") || normContent.contains("mb bank") || normContent.contains("mb-bank")) bankName = "MB Bank";
            else if (normContent.contains("vietcombank") || normContent.contains("vcb")) bankName = "Vietcombank";
            else if (normContent.contains("techcombank")) bankName = "Techcombank";
            else if (normContent.contains("vpbank")) bankName = "VPBank";
            else if (normContent.contains("agribank")) bankName = "Agribank";
            else if (normContent.contains("tpbank")) bankName = "TPBank";
            else if (normContent.contains("acb")) bankName = "ACB";
            else if (normContent.contains("momo")) bankName = "Ví MoMo";
            else if (normContent.contains("zalopay")) bankName = "Ví ZaloPay";
            else if (normContent.contains("viettel")) bankName = "Viettel Money";
            else if (normContent.contains("vietinbank")) bankName = "VietinBank";
            else if (normContent.contains("sacombank")) bankName = "Sacombank";
            else if (normContent.contains("cake")) bankName = "Cake";
            else if (normContent.contains("timo")) bankName = "Timo";
            else if (title.trim().length() > 0 && title.length() <= 25) {
                bankName = title.trim();
            }
        }

        // 2. Loại bỏ chỉ phần số dư tài khoản cuối (ví dụ "SD: 1,500,000VND", "So du cuoi: 12,345,000 VND")
        // để không bóc nhầm số dư thành số tiền giao dịch
        String cleanText = fullContent.replaceAll("(?i)(?:\\bSD\\b(?:\\s*(?:cuối|khả\\s*dụng|hiện\\s*tại|cuoi|kha\\s*dung|hien\\s*tai))?|Số\\s*dư(?:\\s*(?:cuối|khả\\s*dụng|hiện\\s*tại))?|So\\s*du(?:\\s*(?:cuoi|kha\\s*dung|hien\\s*tai))?)\\s*[:=]?\\s*[0-9.,]+\\s*(?:VND|VNĐ|Vnd|vnd|d|đ|dong)?", "");

        // 3. Tìm số tiền và loại giao dịch (INCOME hay EXPENSE)
        String type = "EXPENSE";
        double amount = 0;

        // Ưu tiên Pattern 1: Dấu + hoặc - rõ ràng: "+150,000VND", "- 50.000 đ", "GD -150.000VND", "PS: -150.000VND"
        // Tìm trực tiếp trên fullContent vì số dư tài khoản KHÔNG BAO GIỜ có dấu + hoặc -
        Pattern patternWithSign = Pattern.compile("([+-])\\s*([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,})\\s*(?:VND|VNĐ|Vnd|vnd|d|đ|dong)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = patternWithSign.matcher(fullContent);

        if (matcher.find()) {
            String sign = matcher.group(1);
            String numStr = matcher.group(2).replaceAll("[^0-9]", "");
            try {
                amount = Double.parseDouble(numStr);
                type = "+".equals(sign) ? "INCOME" : "EXPENSE";
            } catch (Exception ignored) {}
        }

        // Pattern 2: Tìm số tiền sau từ khóa giao dịch (Ghi có, Ghi nợ, GD, PS, thanh toan, chi, cong, tru...)
        if (amount <= 0) {
            Pattern patternKw = Pattern.compile("(?i)(?:ghi\\s*nợ|ghi\\s*no|ghi\\s*có|ghi\\s*co|thanh\\s*toán|thanh\\s*toan|chuyển|chuyen|chi|trừ|tru|rút|rut|nhận|nhan|cộng|cong|tăng|tang|giảm|giam|phát\\s*sinh|phat\\s*sinh|\\bps\\b:?|số\\s*tiền(?:\\s*gd|\\s*giao\\s*dịch|\\s*phát\\s*sinh)?|so\\s*tien(?:\\s*gd|\\s*giao\\s*dich|\\s*phat\\s*sinh)?|gd:?|giao\\s*dịch:?|giao\\s*dich:?)\\s*(?:thành\\s*công)?\\s*[:=]?\\s*([+-]?\\s*(?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,}))\\s*(?:VND|VNĐ|Vnd|vnd|d|đ|dong)?", Pattern.CASE_INSENSITIVE);
            Matcher m2 = patternKw.matcher(cleanText);
            if (m2.find()) {
                String fullMatched = m2.group(0).toLowerCase(Locale.ROOT);
                String numStr = m2.group(1).replaceAll("[^0-9]", "");
                try {
                    amount = Double.parseDouble(numStr);
                    if (fullMatched.contains("nhận") || fullMatched.contains("nhan")
                            || fullMatched.contains("cộng") || fullMatched.contains("cong")
                            || fullMatched.contains("ghi có") || fullMatched.contains("ghi co")
                            || fullMatched.contains("tăng") || fullMatched.contains("tang")
                            || fullMatched.contains("+")
                            || normContent.contains("ghi co") || normContent.contains("cong tien") || normContent.contains("nhan tien") || normContent.contains("chuyen den")) {
                        type = "INCOME";
                    } else {
                        type = "EXPENSE";
                    }
                } catch (Exception ignored) {}
            }
        }

        // Pattern 3: Tìm số tiền có kèm đơn vị VND / VNĐ / đ
        if (amount <= 0) {
            Pattern patternVnd = Pattern.compile("([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,})\\s*(?:VND|VNĐ|Vnd|vnd|dong|đ)", Pattern.CASE_INSENSITIVE);
            Matcher m3 = patternVnd.matcher(cleanText);
            if (m3.find()) {
                String numStr = m3.group(1).replaceAll("[^0-9]", "");
                try {
                    amount = Double.parseDouble(numStr);
                    if (normContent.contains("ghi co") || normContent.contains("cong tien") || normContent.contains("nhan tien") || normContent.contains("chuyen den")) {
                        type = "INCOME";
                    } else {
                        type = "EXPENSE";
                    }
                } catch (Exception ignored) {}
            }
        }

        // Nếu không trích xuất được số tiền hợp lệ (> 0) thì không phải thông báo giao dịch
        if (amount <= 0) {
            return null;
        }

        // 4. Bóc tách nội dung ghi chú (Note)
        String note = extractNote(fullContent, bankName);

        // 5. Phân loại danh mục thông minh (Category)
        String categoryName = classifyCategory(type, note, normContent);

        ParsedBankTransaction result = new ParsedBankTransaction();
        result.bankName = bankName;
        result.type = type;
        result.amount = amount;
        result.note = note;
        result.categoryName = categoryName;
        result.rawContent = fullContent.trim();

        return result;
    }

    private static String extractNote(String text, String bankName) {
        // Tìm sau các tiền tố ND:, Nội dung:, Dien giai:, Mo ta:, Ly do:...
        Pattern notePattern = Pattern.compile(
                "(?i)(?:\\bND\\b|\\bNoi\\s*dung\\b|\\bNội\\s*dung\\b|\\bDien\\s*giai\\b|\\bDiễn\\s*giải\\b|\\bMo\\s*ta\\b|\\bMô\\s*tả\\b|\\bLy\\s*do\\b|\\bLý\\s*do\\b)\\s*[:=]?\\s*([^\\n;|]+)"
        );
        Matcher m = notePattern.matcher(text);
        if (m.find()) {
            String note = m.group(1).trim();
            // Cắt bỏ phần số dư hoặc mã tham chiếu nếu nằm cùng dòng sau dấu chấm hoặc khoảng trắng
            note = note.replaceAll("(?i)\\s*[.,]?\\s*(?:SD|So\\s*du|Số\\s*dư|Ref|Ma\\s*GD|Mã\\s*GD).*$", "");
            note = note.replaceAll("[.;,\\s]+$", "").trim();
            if (!note.isEmpty()) {
                return note;
            }
        }

        Pattern atPattern = Pattern.compile(
                "(?i)(?:(?:\\btại\\b|\\btai\\b)(?!\\s*(?:khoản|khoan|ngân\\s*hàng|ngan\\s*hang|bidv|vcb|mb|tpbank|acb|techcombank|vietinbank))|(?:\\bcho\\b|\\btừ\\b|\\btu\\b))\\s+([^\\n;|]+)"
        );
        Matcher m2 = atPattern.matcher(text);
        if (m2.find()) {
            String note = m2.group(1).trim();
            note = note.replaceAll("(?i)\\s*[.,]?\\s*(?:SD|So\\s*du|Số\\s*dư|Ref|Ma\\s*GD|Mã\\s*GD).*$", "");
            note = note.replaceAll("[.;,\\s]+$", "").trim();
            if (!note.isEmpty()) {
                return note;
            }
        }

        return "Giao dịch qua " + bankName;
    }

    private static final Pattern PAT_INCOME_SALARY = Pattern.compile("(?i)\\b(?:luong|salary|thu\\s*nhap|tam\\s*ung)\\b");
    private static final Pattern PAT_INCOME_BONUS = Pattern.compile("(?i)\\b(?:thuong|bonus|hoa\\s*hong)\\b");
    private static final Pattern PAT_INCOME_GIFT = Pattern.compile("(?i)\\b(?:cho|tang|li\\s*xi|mung\\s*tuoi|sinh\\s*nhat)\\b");

    private static final Pattern PAT_BILL = Pattern.compile("(?i)\\b(?:dien(?!\\s*giai\\b)|nuoc|mang|internet|viettel|vnpt|fpt|truyen\\s*hinh|hoa\\s*don|cuoc|phi\\s*quan\\s*ly|evn|dien\\s*luc|cap\\s*nuoc)\\b");
    private static final Pattern PAT_FOOD = Pattern.compile("(?i)(?:(?<!\\.)\\bcom(?!\\.)\\b|\\b(?:pho|bun|an\\s*uong|an\\s*trua|an\\s*toi|an\\s*sang|banh\\s*mi|do\\s*an|thuc\\s*an|an|uong|coffee|cafe|ca\\s*phe|highlands|starbucks|phuc\\s*long|tra\\s*sua|food|pizza|kfc|lotteria|burger|nha\\s*hang|quan\\s*an|quan\\s*nuoc|quan\\s*cafe|grabfood|shopeefood|baemin)\\b)");
    private static final Pattern PAT_TRANSPORT = Pattern.compile("(?i)\\b(?:xang|do\\s*xang|xe|grab|be|gojek|taxi|ve\\s*xe|gui\\s*xe|mai\\s*linh|vinfast|xanh\\s*sm|epass|vetc|thu\\s*phi)\\b");
    private static final Pattern PAT_SHOPPING = Pattern.compile("(?i)\\b(?:shopee|lazada|tiki|tiktok|shop|quan\\s*ao|giay|dep|thoi\\s*trang|tui\\s*xach|sieu\\s*thi|mart|winmart|circle\\s*k|gs25|bach\\s*hoa|co\\.op|big\\s*c|emart|mua\\s*sam|mua\\s*hang)\\b");
    private static final Pattern PAT_HEALTH = Pattern.compile("(?i)\\b(?:thuoc|benh\\s*vien|phong\\s*kham|bac\\s*si|pharmacy|pharmacity|long\\s*chau|an\\s*khang|nha\\s*khoa|y\\s*te|kham\\s*benh)\\b");
    private static final Pattern PAT_EDUCATION = Pattern.compile("(?i)\\b(?:hoc|sach|vo|hoc\\s*phi|khoa\\s*hoc|toeic|ielts|truong|fahasa)\\b");
    private static final Pattern PAT_ENTERTAINMENT = Pattern.compile("(?i)\\b(?:phim|xem\\s*phim|game|cgv|bhd|lotte|galaxy|steam|garena|netflix|spotify|du\\s*lich|booking|travel|agoda)\\b");
    private static final Pattern PAT_HOUSING = Pattern.compile("(?i)\\b(?:tien\\s*nha|thue\\s*nha|phong\\s*tro|sua\\s*nha)\\b");

    private static String classifyCategory(String type, String note, String normFull) {
        String combined = (note + " " + normFull).toLowerCase(Locale.ROOT);
        String norm = AiAssistantManager.stripAccents(combined);

        if ("INCOME".equalsIgnoreCase(type)) {
            if (PAT_INCOME_SALARY.matcher(norm).find()) {
                return "Lương";
            } else if (PAT_INCOME_BONUS.matcher(norm).find()) {
                return "Thưởng";
            } else if (PAT_INCOME_GIFT.matcher(norm).find()) {
                return "Tiền được cho";
            }
            return "Thu nhập khác";
        } else {
            // Chi tiêu (EXPENSE)
            // 1. Hóa đơn (ưu tiên nhận diện trước khi quét từ chung)
            if (PAT_BILL.matcher(norm).find()) {
                return "Hóa đơn";
            }

            // 2. Ăn uống (dùng word boundary để không nhầm toan/hoan/khoan thành an)
            if (PAT_FOOD.matcher(norm).find()) {
                return "Ăn uống";
            }

            // 3. Đi lại (dùng word boundary để không nhầm xem thành xe)
            if (PAT_TRANSPORT.matcher(norm).find()) {
                return "Đi lại";
            }

            // 4. Mua sắm
            if (PAT_SHOPPING.matcher(norm).find()) {
                return "Mua sắm";
            }

            // 5. Sức khỏe
            if (PAT_HEALTH.matcher(norm).find()) {
                return "Sức khỏe";
            }

            // 6. Học tập
            if (PAT_EDUCATION.matcher(norm).find()) {
                return "Học tập";
            }

            // 7. Giải trí
            if (PAT_ENTERTAINMENT.matcher(norm).find()) {
                return "Giải trí";
            }

            // 8. Nhà ở
            if (PAT_HOUSING.matcher(norm).find()) {
                return "Nhà ở";
            }

            return "Chi tiêu khác";
        }
    }

    private static int indexOfIgnoreCase(String str, String target) {
        if (str == null || target == null) return -1;
        return str.toLowerCase(Locale.ROOT).indexOf(target.toLowerCase(Locale.ROOT));
    }
}
