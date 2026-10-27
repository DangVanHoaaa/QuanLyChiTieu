package com.example.quanlychitieu.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.quanlychitieu.MainActivity;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.database.AppDatabase;
import com.example.quanlychitieu.model.Category;
import com.example.quanlychitieu.model.PendingTransaction;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BankNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "BankNotifListener";
    public static final String PREF_NAME = "bank_sync_prefs";
    public static final String KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled";
    public static final String CHANNEL_ID = "bank_transactions_channel";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Biến chống lặp giao dịch (duplicate prevention) trong 15 giây
    private static long lastTransactionTime = 0;
    private static double lastTransactionAmount = 0;
    private static String lastTransactionType = "";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true);
        if (!isEnabled) {
            return;
        }

        // Không xử lý thông báo do chính app mình tạo ra để tránh vòng lặp vô tận
        String packageName = sbn.getPackageName();
        if (getPackageName().equals(packageName)) {
            return;
        }

        Bundle extras = sbn.getNotification().extras;
        if (extras == null) return;

        CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigTextCs = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        CharSequence subTextCs = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        CharSequence[] textLinesCs = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        CharSequence tickerCs = sbn.getNotification().tickerText;

        StringBuilder sb = new StringBuilder();
        if (textCs != null) sb.append(textCs).append(" ");
        if (bigTextCs != null && !bigTextCs.toString().equals(textCs != null ? textCs.toString() : "")) {
            sb.append(bigTextCs).append(" ");
        }
        if (subTextCs != null) sb.append(subTextCs).append(" ");
        if (tickerCs != null) sb.append(tickerCs).append(" ");
        if (textLinesCs != null) {
            for (CharSequence line : textLinesCs) {
                if (line != null) sb.append(line).append(" ");
            }
        }

        String title = titleCs != null ? titleCs.toString() : "";
        String text = sb.toString().trim();

        // Kiểm tra xem có phải thông báo ngân hàng / biến động số dư không
        if (!BankNotificationParser.isBankingNotification(packageName, title, text)) {
            return;
        }

        // Bóc tách giao dịch
        BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(packageName, title, text);
        if (parsed == null || parsed.amount <= 0) {
            return;
        }

        // Xử lý lưu vào danh sách chờ duyệt và phát thông báo trên background thread
        Context appContext = getApplicationContext();
        executor.execute(() -> {
            saveToPendingAndNotifyUser(
                    appContext,
                    parsed.bankName,
                    parsed.type,
                    parsed.amount,
                    parsed.categoryName,
                    parsed.note,
                    parsed.rawContent
            );
        });
    }

    /**
     * Lưu giao dịch ngân hàng vào bảng chờ duyệt (Pending) và phát thông báo để người dùng duyệt & phân loại
     */
    public static PendingTransaction saveToPendingAndNotifyUser(Context context, String bankName, String type, double amount,
                                                  String suggestedCategory, String note, String rawContent) {
        long now = System.currentTimeMillis();

        AppDatabase db = AppDatabase.getInstance(context);

        PendingTransaction pending = new PendingTransaction(
                bankName,
                type,
                amount,
                suggestedCategory,
                rawContent != null ? rawContent : note,
                note,
                DateUtils.getTodayIso(),
                now,
                "PENDING"
        );

        long pendingId = db.pendingTransactionDao().insert(pending);
        pending.setId((int) pendingId);
        Log.i(TAG, "Đã lưu giao dịch chờ duyệt id=" + pendingId + " từ " + bankName);

        // Bắn thông báo nhắc người dùng duyệt & phân loại
        showPendingApprovalNotification(context, pending);
        return pending;
    }

    public static PendingTransaction processAndRecordBankTransaction(Context context, String bankName, String type, double amount,
                                                                      String categoryName, String note) {
        return saveToPendingAndNotifyUser(context, bankName, type, amount, categoryName, note, note);
    }

    /**
     * Bắn thông báo: Phát hiện giao dịch ngân hàng -> Chạm để duyệt & phân loại danh mục
     */
    public static void showPendingApprovalNotification(Context context, PendingTransaction pending) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // Tạo Notification Channel (Yêu cầu từ Android 8.0 / API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Giao dịch ngân hàng cần duyệt",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo khi phát hiện giao dịch ngân hàng cần bạn duyệt và phân loại");
            channel.enableVibration(true);
            channel.enableLights(true);
            nm.createNotificationChannel(channel);
        }

        // Intent mở màn hình Duyệt giao dịch ngân hàng
        Intent intent = new Intent(context, com.example.quanlychitieu.ui.PendingApprovalActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("OPEN_PENDING_APPROVAL", true);
        intent.putExtra("PENDING_ID", pending.getId());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                pending.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        boolean isIncome = "INCOME".equalsIgnoreCase(pending.getType());
        String typeLabel = isIncome ? "Thu nhập" : "Chi tiêu";
        String amountFormatted = (isIncome ? "+ " : "- ") + CurrencyUtils.formatVnd(pending.getAmount());

        String title = "Ví Thông Minh - Giao dịch cần duyệt (" + pending.getBankName() + ")";
        String content = "Phát hiện " + typeLabel + " " + amountFormatted + ". Bấm để chọn danh mục & duyệt vào ví.";

        String bigText = "🔔 Phát hiện giao dịch từ ngân hàng " + pending.getBankName() + "!\n\n"
                + "• Số tiền: " + amountFormatted + " (" + typeLabel + ")\n"
                + "• Gợi ý danh mục: " + pending.getSuggestedCategory() + "\n"
                + "• Nội dung: " + pending.getNote() + "\n\n"
                + "👉 Chạm vào thông báo để tự phân loại danh mục và duyệt vào ví tiền của bạn.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bank)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_check_circle, "DUYỆT & PHÂN LOẠI", pendingIntent)
                .setColor(context.getResources().getColor(R.color.primary));

        nm.notify(pending.getId(), builder.build());
    }

    /**
     * Hàm chính thức duyệt giao dịch và lưu vào ví
     */
    public static void approveAndSaveTransaction(Context context, PendingTransaction pending,
                                                int categoryId, String categoryName,
                                                String note, double amount, String date) {
        AppDatabase db = AppDatabase.getInstance(context);

        Transaction transaction = new Transaction(
                pending.getType(),
                amount,
                categoryId,
                categoryName,
                date,
                note,
                System.currentTimeMillis()
        );

        // Lưu vào bảng transactions chính
        db.transactionDao().insert(transaction);

        // Xóa hoặc đổi trạng thái giao dịch chờ duyệt
        db.pendingTransactionDao().deleteById(pending.getId());

        // Hủy thông báo của giao dịch chờ duyệt này
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(pending.getId());
        }

        Log.i(TAG, "Đã duyệt giao dịch ngân hàng thành công: " + amount + " vào danh mục " + categoryName);
    }
}
