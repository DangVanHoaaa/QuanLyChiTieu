package com.example.quanlychitieu.ai;

import android.content.Context;

import com.example.quanlychitieu.database.AppDatabase;
import com.example.quanlychitieu.model.Budget;
import com.example.quanlychitieu.model.Category;
import com.example.quanlychitieu.model.ChatMessage;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class AiAssistantManager {

    public interface AiResponseCallback {
        void onResponse(ChatMessage response);
        void onError(String errorMessage);
    }

    private final Context context;
    private final AppDatabase db;
    private final ExecutorService executor;
    private String proxyServerUrl = ""; // Có thể cấu hình URL máy chủ trung gian (không lưu API key trong app)

    public AiAssistantManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void setProxyServerUrl(String url) {
        this.proxyServerUrl = url;
    }

    public void processUserMessage(String messageText, AiResponseCallback callback) {
        executor.execute(() -> {
            try {
                if (messageText == null || messageText.trim().isEmpty()) {
                    callback.onError("Vui lòng nhập nội dung câu hỏi!");
                    return;
                }

                String cleanText = messageText.trim();
                String lowerText = cleanText.toLowerCase(Locale.getDefault());

                // 1. Kiểm tra xem người dùng có muốn ghi chép giao dịch bằng ngôn ngữ tự nhiên không
                // Chỉ kích hoạt khi KHÔNG PHẢI là câu hỏi/tư vấn/trò chuyện
                if (!isQuestionOrAdvice(cleanText, lowerText)) {
                    Transaction proposedTransaction = parseTransactionFromNaturalLanguage(cleanText, lowerText);
                    if (proposedTransaction != null) {
                        String promptText = "Tôi nhận diện được giao dịch của bạn như sau:\n\n"
                                + "• Loại: " + ("INCOME".equals(proposedTransaction.getType()) ? "Thu nhập" : "Chi tiêu") + "\n"
                                + "• Danh mục: " + proposedTransaction.getCategoryName() + "\n"
                                + "• Số tiền: " + CurrencyUtils.formatVnd(proposedTransaction.getAmount()) + "\n"
                                + "• Ngày: " + DateUtils.formatDisplayDate(proposedTransaction.getDate()) + "\n"
                                + (proposedTransaction.getNote().isEmpty() ? "" : ("• Ghi chú: " + proposedTransaction.getNote() + "\n\n"))
                                + "Bạn có muốn lưu giao dịch này vào sổ chi tiêu không?";

                        ChatMessage botMsg = new ChatMessage(ChatMessage.SENDER_BOT, promptText, proposedTransaction);
                        callback.onResponse(botMsg);
                        return;
                    }
                }

                // 2. Nếu có cấu hình Google Gemini API hoặc Máy chủ trung gian, gọi AI nâng cao
                String geminiApiKey = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
                        .getString("gemini_api_key", "");

                if (!geminiApiKey.trim().isEmpty()) {
                    String geminiResponse = callGeminiApi(cleanText, geminiApiKey, buildFinancialSummaryContext());
                    if (geminiResponse != null && !geminiResponse.trim().isEmpty()) {
                        callback.onResponse(new ChatMessage(ChatMessage.SENDER_BOT, cleanResponse(geminiResponse)));
                        return;
                    }
                } else if (proxyServerUrl != null && !proxyServerUrl.trim().isEmpty() && isComplexQuestion(lowerText)) {
                    String proxyResponse = callProxyAiServer(cleanText);
                    if (proxyResponse != null && !proxyResponse.isEmpty()) {
                        callback.onResponse(new ChatMessage(ChatMessage.SENDER_BOT, cleanResponse(proxyResponse)));
                        return;
                    }
                }

                // 3. Phân tích tài chính & Trò chuyện mở rộng (Offline AI Engine thông minh)
                String responseText = analyzeFinancialData(cleanText, lowerText);
                callback.onResponse(new ChatMessage(ChatMessage.SENDER_BOT, cleanResponse(responseText)));

            } catch (Exception e) {
                callback.onError("Đã xảy ra lỗi khi phân tích: " + e.getMessage());
            }
        });
    }

    // Kiểm tra xem tin nhắn có phải là câu hỏi, thắc mắc, trò chuyện hay xin tư vấn không
    public static String stripAccents(String str) {
        if (str == null) return "";
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replaceAll("đ", "d").replaceAll("Đ", "D").toLowerCase(Locale.ROOT);
    }

    private boolean isQuestionOrAdvice(String raw, String lower) {
        if (raw.contains("?") || raw.contains("？")) return true;
        String norm = stripAccents(lower);
        return norm.contains("sao")
                || norm.contains("the nao")
                || norm.contains("lam sao")
                || norm.contains("nhu the nao")
                || norm.contains("co nen")
                || norm.contains("nen lam")
                || norm.contains("nen mua")
                || norm.contains("tu van")
                || norm.contains("loi khuyen")
                || norm.contains("khuyen")
                || norm.contains("goi y")
                || norm.contains("giup toi")
                || norm.contains("duoc khong")
                || norm.contains("khong nhi")
                || norm.contains("tai sao")
                || norm.contains("bao nhieu")
                || norm.contains("dau tu")
                || norm.contains("ke hoach")
                || norm.contains("muc tieu")
                || norm.contains("chien luoc")
                || norm.contains("ban la ai")
                || norm.contains("chao")
                || norm.contains("hello")
                || norm.contains("hi bot")
                || norm.contains("dien thoai")
                || norm.contains("laptop")
                || norm.contains("mua xe")
                || norm.contains("mua nha")
                || norm.contains("het tien")
                || norm.contains("chay tui")
                || norm.contains("ket tien")
                || norm.contains("tra no")
                || norm.contains("tiet kiem");
    }

    // Loại bỏ tất cả dấu sao ** in đậm markdown và các format thừa
    private String cleanResponse(String text) {
        if (text == null) return "";
        return text.replace("**", "")
                .replace("### ", "")
                .replace("## ", "")
                .replace("# ", "")
                .trim();
    }

    // --- BÓC TÁCH GIAO DỊCH TỪ NGÔN NGỮ TỰ NHIÊN ---
    private Transaction parseTransactionFromNaturalLanguage(String rawText, String lower) {
        // Phải có ý định chi tiêu hoặc thu nhập rõ ràng (có từ chỉ hành động tiêu hoặc thu tiền)
        boolean hasAction = hasTransactionAction(lower);
        if (!hasAction) {
            return null;
        }

        // Tìm số tiền
        double amount = extractAmount(lower);
        if (amount <= 0) {
            return null;
        }

        // Xác định loại Thu nhập hay Chi tiêu
        boolean isIncome = lower.contains("lương")
                || lower.contains("thưởng")
                || lower.contains("nhận được")
                || lower.contains("thu nhập")
                || lower.contains("được cho")
                || lower.contains("được tặng")
                || lower.contains("lì xì")
                || lower.contains("tiền vào");

        String type = isIncome ? "INCOME" : "EXPENSE";

        // Xác định danh mục phù hợp
        List<Category> categories = db.categoryDao().getCategoriesByTypeSync(type);
        Category matchedCategory = null;

        if (isIncome) {
            if (lower.contains("thưởng")) {
                matchedCategory = findCategoryByName(categories, "Thưởng");
            } else if (lower.contains("lương")) {
                matchedCategory = findCategoryByName(categories, "Lương");
            } else if (lower.contains("cho") || lower.contains("tặng") || lower.contains("lì xì")) {
                matchedCategory = findCategoryByName(categories, "Tiền được cho");
            } else {
                matchedCategory = findCategoryByName(categories, "Thu nhập khác");
            }
        } else {
            if (lower.contains("ăn") || lower.contains("uống") || lower.contains("cơm") || lower.contains("phở")
                    || lower.contains("bún") || lower.contains("trà sữa") || lower.contains("cafe") || lower.contains("cà phê")
                    || lower.contains("bánh") || lower.contains("nhậu") || lower.contains("trưa") || lower.contains("tối") || lower.contains("sáng")) {
                matchedCategory = findCategoryByName(categories, "Ăn uống");
            } else if (lower.contains("xăng") || lower.contains("xe") || lower.contains("grab") || lower.contains("taxi")
                    || lower.contains("bus") || lower.contains("bảo dưỡng") || lower.contains("gửi xe")) {
                matchedCategory = findCategoryByName(categories, "Đi lại");
            } else if (lower.contains("mua") || lower.contains("sắm") || lower.contains("quần") || lower.contains("áo")
                    || lower.contains("giày") || lower.contains("shopee") || lower.contains("lazada") || lower.contains("tiki")) {
                matchedCategory = findCategoryByName(categories, "Mua sắm");
            } else if (lower.contains("điện") || lower.contains("nước") || lower.contains("mạng") || lower.contains("wifi")
                    || lower.contains("internet") || lower.contains("hóa đơn") || lower.contains("rác")) {
                matchedCategory = findCategoryByName(categories, "Hóa đơn");
            } else if (lower.contains("học") || lower.contains("sách") || lower.contains("vở") || lower.contains("khóa học")
                    || lower.contains("học phí")) {
                matchedCategory = findCategoryByName(categories, "Học tập");
            } else if (lower.contains("phim") || lower.contains("game") || lower.contains("du lịch") || lower.contains("chơi")) {
                matchedCategory = findCategoryByName(categories, "Giải trí");
            } else if (lower.contains("thuốc") || lower.contains("khám") || lower.contains("bệnh") || lower.contains("bác sĩ")
                    || lower.contains("nha khoa")) {
                matchedCategory = findCategoryByName(categories, "Sức khỏe");
            } else if (lower.contains("tiền nhà") || lower.contains("thuê nhà") || lower.contains("phòng trọ") || lower.contains("sửa nhà")) {
                matchedCategory = findCategoryByName(categories, "Nhà ở");
            } else {
                matchedCategory = findCategoryByName(categories, "Chi tiêu khác");
            }
        }

        if (matchedCategory == null && !categories.isEmpty()) {
            matchedCategory = categories.get(0);
        }

        int catId = matchedCategory != null ? matchedCategory.getId() : 1;
        String catName = matchedCategory != null ? matchedCategory.getName() : "Chi tiêu khác";

        // Ghi chú là nội dung nguyên bản rút gọn
        String note = rawText;
        if (note.length() > 60) {
            note = note.substring(0, 60);
        }

        return new Transaction(type, amount, catId, catName, DateUtils.getTodayIso(), note, System.currentTimeMillis());
    }

    private boolean hasTransactionAction(String lower) {
        return lower.contains("ăn") || lower.contains("uống") || lower.contains("mua")
                || lower.contains("chi") || lower.contains("trả") || lower.contains("tiêu")
                || lower.contains("hết") || lower.contains("đổ xăng") || lower.contains("gửi xe")
                || lower.contains("lương") || lower.contains("thưởng") || lower.contains("được cho")
                || lower.contains("được tặng") || lower.contains("lì xì") || lower.contains("nhận")
                || lower.contains("đóng") || lower.contains("nạp") || lower.contains("rút")
                || lower.contains("vừa") || lower.contains("mới") || lower.contains("hôm nay")
                || lower.contains("tốn") || lower.contains("mất");
    }

    private Category findCategoryByName(List<Category> list, String name) {
        if (list == null) return null;
        for (Category c : list) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    private double extractAmount(String text) {
        // Regex bóc tách số tiền: "45k", "45 nghìn", "45 ngàn", "1.5 triệu", "1,5tr", "50.000", "50000"
        Pattern pattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|nghìn|ngàn|k|đ|vnd|dong)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            try {
                String numStr = matcher.group(1).replace(",", ".");
                String unit = matcher.group(2) != null ? matcher.group(2).toLowerCase() : "";

                double val = Double.parseDouble(numStr);

                if (unit.contains("triệu") || unit.equals("tr")) {
                    return val * 1000000.0;
                } else if (unit.contains("nghìn") || unit.contains("ngàn") || unit.equals("k")) {
                    return val * 1000.0;
                } else if (val >= 1000) {
                    return val;
                } else if (unit.equals("đ") || unit.equals("vnd")) {
                    return val;
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    // --- PHÂN TÍCH TÀI CHÍNH & TRÒ CHUYỆN MỞ RỘNG (OFFLINE AI ENGINE) ---
    private String analyzeFinancialData(String rawText, String lower) {
        String norm = stripAccents(lower);
        String currentMonth = DateUtils.getCurrentMonth();
        Double totalIncome = db.transactionDao().getMonthlyIncomeSync(currentMonth);
        Double totalExpense = db.transactionDao().getMonthlyExpenseSync(currentMonth);
        if (totalIncome == null) totalIncome = 0.0;
        if (totalExpense == null) totalExpense = 0.0;
        double balance = totalIncome - totalExpense;

        List<Transaction> monthTransactions = db.transactionDao().getTransactionsByMonthSync(currentMonth);
        Budget monthlyBudget = db.budgetDao().getTotalBudgetByMonthSync(currentMonth);

        // 1. Lời chào & Hỏi thăm thân mật
        if (norm.equals("chao") || norm.equals("hi") || norm.startsWith("hi ") || norm.contains("hello")
                || norm.contains("alo") || norm.contains("ban oi") || norm.contains("buoi sang") || norm.contains("buoi toi")
                || norm.startsWith("chao ban") || norm.startsWith("xin chao")) {
            return "Chào bạn! Tôi là Trợ lý Tài chính AI của Ví Thông Minh 🤖. Rất vui được gặp bạn!\n\n"
                    + "Hôm nay tình hình chi tiêu của bạn thế nào rồi? Bạn cần tôi phân tích số liệu, gợi ý cách tiết kiệm hay muốn tâm sự về kế hoạch tài chính nào cứ thoải mái chia sẻ nhé!";
        }

        // 2. Giới thiệu bản thân & Năng lực
        if (norm.contains("ban la ai") || norm.contains("ban ten gi") || norm.contains("ai tao ra")
                || norm.contains("lam duoc gi") || norm.contains("gioi thieu") || norm.contains("chuc nang cua ban")) {
            return "Tôi là Trợ lý Tài chính Cá nhân AI đồng hành cùng bạn trong ứng dụng Ví Thông Minh ✨\n\n"
                    + "Tôi có thể giúp bạn:\n"
                    + "- Trò chuyện mở rộng, tư vấn tài chính, đầu tư, mua sắm và giải đáp thắc mắc tiền bạc.\n"
                    + "- Ghi chép thu chi tự động qua lời nhắn (ví dụ: 'Ăn trưa 45k', 'Vừa nhận lương 10 triệu').\n"
                    + "- Báo cáo tổng kết chi tiêu tháng, tìm danh mục tốn kém nhất và so sánh giữa các tháng.\n"
                    + "- Cảnh báo vượt ngân sách và giúp bạn theo dõi các mục tiêu tiết kiệm.\n\n"
                    + "Bạn muốn bắt đầu tìm hiểu điều gì trước nào?";
        }

        // 3. Cảm ơn & Phản hồi tích cực
        if (norm.contains("cam on") || norm.contains("thank") || norm.contains("tuyet voi")
                || norm.contains("gioi qua") || norm.contains("hay qua") || norm.contains("duoc roi") || norm.equals("ok")) {
            return "Dạ không có gì ạ! Rất vui vì đã mang lại thông tin hữu ích cho bạn ❤️.\n\n"
                    + "Quản lý tài chính cá nhân là một chặng đường dài, hãy kiên trì ghi chép mỗi ngày để ví tiền luôn khỏe mạnh bạn nhé!";
        }

        // 4. Đồng cảm khi người dùng khó khăn, hết tiền, cháy túi, áp lực
        if (norm.contains("het tien") || norm.contains("chay tui") || norm.contains("viem mang tui")
                || norm.contains("ngheo") || norm.contains("ap luc") || norm.contains("lo lang") || norm.contains("buon") || norm.contains("ket tien")) {
            return "Đừng quá căng thẳng bạn nhé, ai trong chúng ta cũng từng trải qua những thời điểm tài chính eo hẹp như vậy 🤗.\n\n"
                    + "Để nhanh chóng vượt qua giai đoạn này, bạn hãy thử 4 bước sơ cứu ví tiền:\n"
                    + "1. Dừng ngay mua sắm tùy hứng: Tránh xa các trang thương mại điện tử và những buổi cà phê, tụ tập tốn kém trong 1-2 tuần tới.\n"
                    + "2. Tận dụng đồ có sẵn: Nấu ăn tại nhà với thực phẩm sẵn có trong tủ lạnh, ưu tiên các món thanh đạm, tiết kiệm.\n"
                    + "3. Chia đều số dư: Kiểm tra số tiền còn lại và chia đều cho số ngày từ nay đến kỳ nhận lương tiếp theo để biết hạn mức tối đa mỗi ngày.\n"
                    + "4. Ghi chép từng đồng nhỏ: Vào app ghi chép mọi khoản chi từ 10k - 20k để phát hiện ngay các khoản rò rỉ.\n\n"
                    + "Bạn còn khó khăn nào nữa không, hãy chia sẻ cùng tôi nhé!";
        }

        // 5. Tư vấn mua sắm lớn (Mua xe, Mua nhà, Mua điện thoại, Laptop)
        if (norm.contains("mua xe") || norm.contains("mua o to") || norm.contains("mua oto") || norm.contains("mua xe may")
                || norm.contains("mua nha") || norm.contains("mua chung cu") || norm.contains("dien thoai")
                || norm.contains("iphone") || norm.contains("laptop") || norm.contains("macbook")) {
            if (norm.contains("dien thoai") || norm.contains("iphone") || norm.contains("laptop") || norm.contains("macbook")) {
                return "💡 Lời khuyên trước khi mua đồ công nghệ đắt tiền:\n\n"
                        + "- Phân biệt Nhu cầu và Mong muốn: Chiếc máy hiện tại có còn phục vụ tốt công việc hay không?\n"
                        + "- Quy tắc 50%: Giá trị món đồ không nên vượt quá 50% tổng thu nhập hàng tháng của bạn. Nếu vượt quá, hãy vào tab 'Tiết kiệm' trong app, tạo mục tiêu và trích tiền tích lũy trong 3-4 tháng thay vì mua trả góp dồn dập.\n"
                        + "- Quy tắc 7 ngày: Đợi ít nhất 7 ngày sau khi có ý định mua, nếu sau 7 ngày bạn vẫn thực sự cần nó thì mới đưa ra quyết định.";
            } else {
                return "🏠🚗 Lời khuyên cho các quyết định mua sắm tài sản lớn (Nhà / Xe):\n\n"
                        + "- Quy tắc mua xe (20/4/10): Trả trước tối thiểu 20%, thời hạn vay tối đa 4 năm, tổng chi phí nuôi xe hàng tháng (xăng, bảo dưỡng, gửi xe, trả góp) không quá 10% thu nhập.\n"
                        + "- Quy tắc mua nhà: Bạn nên chuẩn bị sẵn vốn tự có từ 30% - 50% giá trị căn nhà. Khoản trả gốc và lãi ngân hàng hàng tháng tối đa chỉ nên chiếm 30% - 40% tổng thu nhập gia đình để phòng ngừa rủi ro lãi suất thả nổi.\n\n"
                        + "👉 Hãy vào tab 'Tiết kiệm' trong app để đặt ngay mục tiêu tích lũy khoản tiền đặt cọc ban đầu này nhé!";
            }
        }

        // 6. Tư vấn đầu tư & Quản lý tiền nhàn rỗi
        if (norm.contains("dau tu") || norm.contains("gui tiet kiem") || norm.contains("mua vang") || norm.contains("vang")
                || norm.contains("chung khoan") || norm.contains("bat dong san") || norm.contains("lai suat") || norm.contains("tien nhan roi") || norm.contains("lai kep")) {
            return "📈 Nguyên tắc đầu tư tài chính cá nhân thông minh:\n\n"
                    + "1. Xây dựng Quỹ khẩn cấp trước tiên: Đảm bảo có khoản tiền gửi tiết kiệm tương đương 3 đến 6 tháng sinh hoạt phí trước khi nghĩ đến đầu tư rủi ro.\n"
                    + "2. Phân bổ theo khẩu vị rủi ro:\n"
                    + "   - An toàn, thanh khoản cao: Gửi tiết kiệm kỳ hạn ngắn, tích lũy vàng từng chỉ.\n"
                    + "   - Tăng trưởng dài hạn: Quỹ mở (Mutual Funds), chứng chỉ quỹ cổ phiếu (phù hợp dòng tiền nhàn rỗi từ 3 năm trở lên để hưởng sức mạnh lãi kép).\n"
                    + "3. Nguyên tắc vàng: Không bỏ tất cả trứng vào một giỏ và tuyệt đối không vay mượn tiền để đầu tư lướt sóng.";
        }

        // 7. Quản lý nợ & Thẻ tín dụng
        if (norm.contains("no ") || norm.contains("tra no") || norm.contains("the tin dung") || norm.contains("vay tien") || norm.contains("vay muon")) {
            return "💳 Chiến lược thoát nợ và dùng thẻ tín dụng hiệu quả:\n\n"
                    + "1. Hai phương pháp trả nợ kinh điển:\n"
                    + "   - Quả cầu tuyết (Snowball): Trả hết món nợ nhỏ nhất trước để giải phóng áp lực tâm lý và tạo động lực.\n"
                    + "   - Tuyết lở (Avalanche): Tập trung dồn tiền trả món nợ có lãi suất cao nhất trước để tiết kiệm tối đa tiền lãi phát sinh.\n"
                    + "2. Dùng thẻ tín dụng an toàn: Luôn thanh toán 100% dư nợ đúng hạn trong kỳ sao kê để không bị phạt lãi suất 25-35%/năm.";
        }

        // 8. Tăng thu nhập & Nghề tay trái
        if (norm.contains("tang thu nhap") || norm.contains("kiem them") || norm.contains("lam them") || norm.contains("freelance") || norm.contains("nghe tay trai")) {
            return "🚀 3 cách giúp bạn gia tăng thu nhập bền vững:\n\n"
                    + "1. Nâng cao giá trị công việc chính: Học thêm kỹ năng chuyên môn, đề xuất các dự án mới để có cơ hội tăng lương hoặc thưởng hiệu suất.\n"
                    + "2. Phát triển công việc làm thêm (Side Hustle): Tận dụng chuyên môn hoặc sở trường (viết lách, đồ họa, lập trình, bán hàng online, dạy kèm...).\n"
                    + "3. Thanh lý đồ đạc không dùng: Dọn dẹp lại nhà cửa và bán lại các đồ cũ còn tốt để có ngay một khoản tiền mặt dự phòng.";
        }

        // 9. Quy tắc 50/30/20 và Phương pháp 6 chiếc lọ
        if (norm.contains("50/30/20") || norm.contains("6 chiec lo") || norm.contains("chiec lo") || norm.contains("phuong phap quan ly") || norm.contains("quy tac")) {
            return "📚 2 phương pháp quản lý tài chính hiệu quả nhất thế giới:\n\n"
                    + "1. Quy tắc 50/30/20:\n"
                    + "   - 50% cho Nhu cầu thiết yếu: Ăn uống, thuê nhà, điện nước, đi lại.\n"
                    + "   - 30% cho Sở thích cá nhân: Mua sắm, giải trí, ăn ngoài, du lịch.\n"
                    + "   - 20% cho Tiết kiệm & Đầu tư: Quỹ dự phòng, trả nợ, gửi tiết kiệm.\n\n"
                    + "2. Phương pháp 6 chiếc lọ (Jars System):\n"
                    + "   - 55% Nhu cầu thiết yếu (NEC)\n"
                    + "   - 10% Tự do tài chính (FFA)\n"
                    + "   - 10% Tiết kiệm dài hạn (LTSS)\n"
                    + "   - 10% Giáo dục (EDU)\n"
                    + "   - 10% Hưởng thụ (PLAY)\n"
                    + "   - 5% Cho đi (GIVE)\n\n"
                    + "👉 Bạn có thể vào tab 'Ngân sách' trong app để đặt hạn mức theo các tỷ lệ này rất dễ dàng!";
        }

        // 10. Danh mục chi tiêu nhiều nhất
        if (norm.contains("nhieu nhat") || norm.contains("ton nhat") || norm.contains("khoan nao lon nhat") || norm.contains("chi nhieu o dau") || norm.contains("danh muc ton tien")) {
            if (totalExpense <= 0 || monthTransactions.isEmpty()) {
                return "Trong " + DateUtils.formatDisplayMonth(currentMonth) + ", bạn chưa có khoản chi tiêu nào nên chưa có danh mục tốn tiền nhất.";
            }

            Map<String, Double> categoryMap = new HashMap<>();
            for (Transaction t : monthTransactions) {
                if ("EXPENSE".equalsIgnoreCase(t.getType())) {
                    categoryMap.put(t.getCategoryName(), categoryMap.getOrDefault(t.getCategoryName(), 0.0) + t.getAmount());
                }
            }

            String topCategory = "";
            double maxAmount = 0.0;
            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                if (entry.getValue() > maxAmount) {
                    maxAmount = entry.getValue();
                    topCategory = entry.getKey();
                }
            }

            int percent = (int) Math.round((maxAmount / totalExpense) * 100);
            return "📊 Danh mục bạn chi tiêu nhiều nhất trong " + DateUtils.formatDisplayMonth(currentMonth) + " là:\n\n"
                    + "👉 " + topCategory + ": " + CurrencyUtils.formatVnd(maxAmount) + " (chiếm " + percent + "% tổng chi tiêu tháng).\n\n"
                    + (percent > 40 ? "💡 Lời khuyên: Danh mục này đang chiếm tỷ trọng khá lớn, bạn nên chú ý cân đối nhé!" : "✅ Tỷ trọng chi tiêu này vẫn ở mức an toàn.");
        }

        // 11. So sánh chi tiêu với tháng trước
        if (norm.contains("so sanh") || norm.contains("thang truoc") || norm.contains("tang hay giam")) {
            String prevMonth = getPreviousMonth(currentMonth);
            Double prevExpense = db.transactionDao().getMonthlyExpenseSync(prevMonth);
            if (prevExpense == null) prevExpense = 0.0;

            if (prevExpense == 0 && totalExpense == 0) {
                return "Bạn chưa có dữ liệu chi tiêu ở cả " + DateUtils.formatDisplayMonth(currentMonth) + " và " + DateUtils.formatDisplayMonth(prevMonth) + " để so sánh.";
            }

            double diff = totalExpense - prevExpense;
            String resultText = "📈 So sánh chi tiêu giữa 2 tháng gần nhất:\n\n"
                    + "- " + DateUtils.formatDisplayMonth(prevMonth) + ": " + CurrencyUtils.formatVnd(prevExpense) + "\n"
                    + "- " + DateUtils.formatDisplayMonth(currentMonth) + ": " + CurrencyUtils.formatVnd(totalExpense) + "\n\n";

            if (prevExpense == 0) {
                resultText += "Tháng trước bạn chưa ghi chép chi tiêu nào.";
            } else if (diff > 0) {
                int percentIncrease = (int) Math.round((diff / prevExpense) * 100);
                resultText += "⚠️ Bạn đã chi tiêu nhiều hơn tháng trước " + CurrencyUtils.formatVnd(diff) + " (tăng " + percentIncrease + "%).\nHãy rà soát lại các khoản chi không cần thiết nhé!";
            } else if (diff < 0) {
                int percentDecrease = (int) Math.round((Math.abs(diff) / prevExpense) * 100);
                resultText += "🎉 Tuyệt vời! Bạn đã tiết kiệm được " + CurrencyUtils.formatVnd(Math.abs(diff)) + " so với tháng trước (giảm " + percentDecrease + "%). Tiếp tục phát huy nhé!";
            } else {
                resultText += "Chi tiêu tháng này của bạn hoàn toàn ngang bằng với tháng trước.";
            }
            return resultText;
        }

        // 12. Hỏi về ngân sách
        if (norm.contains("ngan sach") || norm.contains("vuot ngan sach") || norm.contains("con lai bao nhieu") || norm.contains("budget")) {
            if (monthlyBudget == null || monthlyBudget.getAmount() <= 0) {
                return "Bạn chưa thiết lập ngân sách tổng cho " + DateUtils.formatDisplayMonth(currentMonth) + ".\n\nHãy vào tab Ngân sách để đặt hạn mức chi tiêu nhé!";
            }

            double budgetAmount = monthlyBudget.getAmount();
            double remaining = budgetAmount - totalExpense;
            int percent = (int) Math.round((totalExpense / budgetAmount) * 100);

            String budgetStatus = "💰 Tình hình ngân sách " + DateUtils.formatDisplayMonth(currentMonth) + ":\n\n"
                    + "- Ngân sách đặt ra: " + CurrencyUtils.formatVnd(budgetAmount) + "\n"
                    + "- Đã chi: " + CurrencyUtils.formatVnd(totalExpense) + " (" + percent + "%)\n"
                    + "- Còn lại: " + (remaining >= 0 ? CurrencyUtils.formatVnd(remaining) : ("Vượt mức: -" + CurrencyUtils.formatVnd(Math.abs(remaining)))) + "\n\n";

            if (totalExpense > budgetAmount) {
                budgetStatus += "🚨 Cảnh báo: Bạn đã vượt quá ngân sách tháng này! Hãy hạn chế tối đa các khoản chi phát sinh.";
            } else if (percent >= 80) {
                budgetStatus += "⚠️ Chú ý: Bạn đã tiêu hết " + percent + "% ngân sách, hãy cân nhắc kỹ trước khi mua sắm thêm.";
            } else {
                budgetStatus += "✅ Ngân sách của bạn đang được kiểm soát rất tốt!";
            }
            return budgetStatus;
        }

        // 13. Mẹo tiết kiệm tiền chung
        if (norm.contains("tiet kiem") || norm.contains("cat giam") || norm.contains("loi khuyen") || norm.contains("tu van")) {
            return "💡 Lời khuyên tài chính dành cho bạn:\n\n"
                    + "1. Nấu ăn tại nhà: Giúp bạn tiết kiệm từ 30% đến 50% chi phí ăn uống so với việc liên tục đặt đồ ăn ngoài.\n"
                    + "2. Áp dụng quy tắc 24 giờ: Trước khi quyết định mua một món đồ không cấp thiết, hãy chờ đúng 24 giờ để cơn hưng phấn mua sắm lắng xuống.\n"
                    + "3. Hủy gói dịch vụ không dùng: Rà soát lại các gói cước, tài khoản xem phim, nghe nhạc mà bạn ít sử dụng.\n"
                    + "4. Trả cho mình trước: Ngay khi vừa nhận lương, hãy trích ngay 10% - 20% vào quỹ tiết kiệm trước khi bắt đầu chi tiêu cho tháng mới.\n\n"
                    + "Tháng này bạn đã tích lũy được: " + CurrencyUtils.formatVnd(Math.max(0, balance)) + ". Hãy cố gắng duy trì nhé!";
        }

        // 14. Phản hồi trò chuyện tổng quát / Báo cáo tài chính mở rộng
        return "Chào bạn! Đây là tình hình tài chính thực tế trong " + DateUtils.formatDisplayMonth(currentMonth) + " của bạn:\n\n"
                + "- 💵 Tổng thu nhập: " + CurrencyUtils.formatVnd(totalIncome) + "\n"
                + "- 💸 Tổng chi tiêu: " + CurrencyUtils.formatVnd(totalExpense) + "\n"
                + "- ⚖️ Số dư hiện tại: " + (balance >= 0 ? "+" : "") + CurrencyUtils.formatVnd(balance) + "\n\n"
                + (balance >= 0
                ? "Dòng tiền tháng này của bạn đang ở trạng thái dương rất tốt! Bạn có dự định tiết kiệm, mua sắm lớn hay cần tôi tư vấn về chủ đề nào không?"
                : "Tháng này chi tiêu đang lớn hơn thu nhập. Bạn có muốn tôi gợi ý một số cách cắt giảm chi phí tức thời không?");
    }

    private String getPreviousMonth(String currentMonth) {
        try {
            Calendar cal = Calendar.getInstance();
            Date date = DateUtils.MONTH_FORMAT.parse(currentMonth);
            if (date != null) cal.setTime(date);
            cal.add(Calendar.MONTH, -1);
            return DateUtils.MONTH_FORMAT.format(cal.getTime());
        } catch (Exception e) {
            return currentMonth;
        }
    }

    private boolean isComplexQuestion(String lower) {
        return lower.length() > 20 && !lower.contains("chi tiêu") && !lower.contains("ngân sách");
    }

    // --- GỌI MÁY CHỦ TRUNG GIAN (PROXY SERVER) ---
    private String callProxyAiServer(String userPrompt) {
        try {
            URL url = new URL(proxyServerUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setDoOutput(true);

            String jsonInput = "{\"prompt\": \"" + userPrompt.replace("\"", "\\\"") + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                }
                return response.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // --- GỌI GOOGLE GEMINI AI ---
    private String callGeminiApi(String userPrompt, String apiKey, String financialContext) {
        try {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            conn.setDoOutput(true);

            String systemInstruction = "Bạn là người bạn đồng hành và trợ lý tài chính cá nhân thông minh của ứng dụng Ví Thông Minh.\n"
                    + "Hãy trò chuyện tự nhiên, cởi mở, thân thiện, trả lời sâu sắc và có thể mở rộng mọi chủ đề liên quan đến tài chính, tiêu dùng, lối sống, tâm lý mua sắm hoặc trò chuyện giao tiếp hàng ngày.\n"
                    + "QUY TẮC BẮT BUỘC:\n"
                    + "1. TUYỆT ĐỐI KHÔNG dùng ký tự ** (hai dấu sao in đậm) trong toàn bộ câu trả lời. Hãy viết văn bản thuần túy, dùng các biểu tượng emoji sinh động hoặc dấu gạch đầu dòng (-) để làm nổi bật ý.\n"
                    + "2. Trả lời bằng tiếng Việt lịch sự, đồng cảm và hữu ích.\n\n"
                    + "Dưới đây là bức tranh tài chính thực tế hiện tại của người dùng để bạn tham khảo khi cần:\n" + financialContext + "\n\n"
                    + "Câu hỏi hoặc lời nhắn của người dùng: " + userPrompt;

            JSONObject part = new JSONObject();
            part.put("text", systemInstruction);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(contentObj);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);

            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                }

                JSONObject resJson = new JSONObject(response.toString());
                JSONArray candidates = resJson.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject first = candidates.getJSONObject(0);
                    JSONObject content = first.optJSONObject("content");
                    if (content != null) {
                        JSONArray resParts = content.optJSONArray("parts");
                        if (resParts != null && resParts.length() > 0) {
                            return resParts.getJSONObject(0).optString("text", "");
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String buildFinancialSummaryContext() {
        String currentMonth = DateUtils.getCurrentMonth();
        Double totalIncome = db.transactionDao().getMonthlyIncomeSync(currentMonth);
        Double totalExpense = db.transactionDao().getMonthlyExpenseSync(currentMonth);
        Budget budget = db.budgetDao().getTotalBudgetByMonthSync(currentMonth);

        return "Tháng hiện tại: " + currentMonth + "\n"
                + "Tổng thu nhập tháng: " + CurrencyUtils.formatVnd(totalIncome != null ? totalIncome : 0) + "\n"
                + "Tổng chi tiêu tháng: " + CurrencyUtils.formatVnd(totalExpense != null ? totalExpense : 0) + "\n"
                + "Ngân sách tháng: " + (budget != null ? CurrencyUtils.formatVnd(budget.getAmount()) : "Chưa đặt") + "\n";
    }
}
