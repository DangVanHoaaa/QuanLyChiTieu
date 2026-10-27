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
                || norm.contains("gd:") || norm.contains("gd ");

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
            if (lowerPkg.contains("mbmobile") || lowerPkg.contains("mbbank") || lowerPkg.contains("mb.android")) {
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
            if (normContent.contains("mbbank") || normContent.contains("mb bank") || normContent.contains("mb-bank")) bankName = "MB Bank";
            else if (normContent.contains("vietcombank") || normContent.contains("vcb")) bankName = "Vietcombank";
            else if (normContent.contains("techcombank")) bankName = "Techcombank";
            else if (normContent.contains("vpbank")) bankName = "VPBank";
            else if (normContent.contains("bidv")) bankName = "BIDV";
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

        // 2. Loại bỏ chỉ phần số dư tài khoản cuối (ví dụ "SD: 1,500,000VND") để không bóc nhầm số dư thành số tiền giao dịch
        // KHÔNG dùng substring("so du") vì cụm "biến động số dư" ở đầu câu sẽ làm mất toàn bộ nội dung!
        String cleanText = fullContent.replaceAll("(?i)(?:\\bSD\\b|Số\\s*dư|So\\s*du)\\s*[:=]?\\s*[0-9.,]+\\s*(?:VND|VNĐ|d|đ|dong)?", "");

        // 3. Tìm số tiền và loại giao dịch (INCOME hay EXPENSE)
        String type = "EXPENSE";
        double amount = 0;

        // Ưu tiên Pattern 1: Dấu + hoặc - rõ ràng: "+150,000VND", "- 50.000 đ", "GD -150.000VND"
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

        // Pattern 2: Tìm số tiền sau từ khóa giao dịch (GD, thanh toan, chi, cong, tru...)
        if (amount <= 0) {
            Pattern patternKw = Pattern.compile("(?:thanh toán|thanh toan|chuyển|chuyen|chi|trừ|tru|rút|rut|nhận|nhan|cộng|cong|số tiền|sotien|gd:?|giao dich:?)\\s*(?:thành công)?\\s*:?\\s*([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,})\\s*(?:VND|VNĐ|Vnd|vnd|d|đ|dong)?", Pattern.CASE_INSENSITIVE);
            Matcher m2 = patternKw.matcher(cleanText);
            if (m2.find()) {
                String fullMatched = m2.group(0).toLowerCase(Locale.ROOT);
                String numStr = m2.group(1).replaceAll("[^0-9]", "");
                try {
                    amount = Double.parseDouble(numStr);
                    if (fullMatched.contains("nhận") || fullMatched.contains("nhan") || fullMatched.contains("cộng") || fullMatched.contains("cong") || fullMatched.contains("+")) {
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
                    type = "EXPENSE";
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
        // Tìm sau các tiền tố ND:, Nội dung:, tai, cho, tu. Ngăn cách bởi dấu chấm, xuống dòng, chấm phẩy hoặc dấu gạch đứng |
        Pattern notePattern = Pattern.compile("(?:ND|Noi dung|Nội dung)\\s*[:=]?\\s*([^.\\n;|]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = notePattern.matcher(text);
        if (m.find()) {
            String note = m.group(1).trim();
            // Nếu có dính cụm SD hoặc Số dư ở đuôi thì cắt bỏ
            note = note.replaceAll("(?i)\\s+(?:SD|So du|Số dư).*$", "").trim();
            if (!note.isEmpty()) {
                return note;
            }
        }

        Pattern atPattern = Pattern.compile("(?:tại|tai|cho|từ|tu)\\s+([^.\\n;|]+)", Pattern.CASE_INSENSITIVE);
        Matcher m2 = atPattern.matcher(text);
        if (m2.find()) {
            String note = m2.group(1).trim();
            note = note.replaceAll("(?i)\\s+(?:SD|So du|Số dư).*$", "").trim();
            if (!note.isEmpty()) {
                return note;
            }
        }

        return "Giao dịch qua " + bankName;
    }

    private static String classifyCategory(String type, String note, String normFull) {
        String combined = (note + " " + normFull).toLowerCase(Locale.ROOT);
        String norm = AiAssistantManager.stripAccents(combined);

        if ("INCOME".equalsIgnoreCase(type)) {
            if (norm.contains("luong") || norm.contains("salary") || norm.contains("thu nhap") || norm.contains("tam ung")) {
                return "Lương";
            } else if (norm.contains("thuong") || norm.contains("bonus") || norm.contains("hoa hong")) {
                return "Thưởng";
            } else if (norm.contains("cho") || norm.contains("tang") || norm.contains("li xi") || norm.contains("mung tuoi")) {
                return "Tiền được cho";
            }
            return "Thu nhập khác";
        } else {
            // Chi tiêu (EXPENSE)
            if (norm.contains("com") || norm.contains("pho") || norm.contains("bun") || norm.contains("an ") || norm.contains("uong")
                    || norm.contains("coffee") || norm.contains("cafe") || norm.contains("highlands") || norm.contains("starbucks")
                    || norm.contains("phuc long") || norm.contains("tra sua") || norm.contains("food") || norm.contains("pizza")
                    || norm.contains("kfc") || norm.contains("lotteria") || norm.contains("burger") || norm.contains("quan")
                    || norm.contains("nha hang") || norm.contains("grabfood") || norm.contains("shopeefood") || norm.contains("baemin")) {
                return "Ăn uống";
            }

            if (norm.contains("xang") || norm.contains("xe") || norm.contains("grab") || norm.contains("be ") || norm.contains("gojek")
                    || norm.contains("taxi") || norm.contains("ve xe") || norm.contains("gui xe") || norm.contains("mai linh")
                    || norm.contains("vinfast") || norm.contains("xanh sm") || norm.contains("epass") || norm.contains("vetc") || norm.contains("thu phi")) {
                return "Đi lại";
            }

            if (norm.contains("shopee") || norm.contains("lazada") || norm.contains("tiki") || norm.contains("tiktok")
                    || norm.contains("shop") || norm.contains("quan ") || norm.contains("ao ") || norm.contains("giay")
                    || norm.contains("sieu thi") || norm.contains("mart") || norm.contains("winmart") || norm.contains("circle k")
                    || norm.contains("gs25") || norm.contains("bach hoa") || norm.contains("co.op") || norm.contains("big c")
                    || norm.contains("emart") || norm.contains("mua sam") || norm.contains("mua hang")) {
                return "Mua sắm";
            }

            if (norm.contains("dien") || norm.contains("nuoc") || norm.contains("mang") || norm.contains("internet")
                    || norm.contains("viettel") || norm.contains("vnpt") || norm.contains("fpt") || norm.contains("truyen hinh")
                    || norm.contains("hoa don") || norm.contains("cuoc") || norm.contains("phi quan ly") || norm.contains("evn")) {
                return "Hóa đơn";
            }

            if (norm.contains("hoc") || norm.contains("sach") || norm.contains("vo") || norm.contains("hoc phi")
                    || norm.contains("khoa hoc") || norm.contains("toeic") || norm.contains("ielts") || norm.contains("truong")) {
                return "Học tập";
            }

            if (norm.contains("phim") || norm.contains("game") || norm.contains("cgv") || norm.contains("bhd")
                    || norm.contains("lotte") || norm.contains("galaxy") || norm.contains("steam") || norm.contains("garena")
                    || norm.contains("netflix") || norm.contains("spotify") || norm.contains("du lich") || norm.contains("booking")
                    || norm.contains("travel") || norm.contains("agoda")) {
                return "Giải trí";
            }

            if (norm.contains("thuoc") || norm.contains("benh vien") || norm.contains("phong kham") || norm.contains("bac si")
                    || norm.contains("pharmacy") || norm.contains("pharmacity") || norm.contains("long chau") || norm.contains("an khang")
                    || norm.contains("nha khoa") || norm.contains("y te")) {
                return "Sức khỏe";
            }

            if (norm.contains("tien nha") || norm.contains("thue nha") || norm.contains("phong tro") || norm.contains("sua nha")) {
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
