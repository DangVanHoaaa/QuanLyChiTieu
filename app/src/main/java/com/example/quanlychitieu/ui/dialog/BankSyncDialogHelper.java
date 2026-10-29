package com.example.quanlychitieu.ui.dialog;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.service.BankNotificationListenerService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BankSyncDialogHelper {

    public static boolean isNotificationServiceEnabled(Context context) {
        String pkgName = context.getPackageName();
        final String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && TextUtils.equals(pkgName, cn.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void showBankSyncDialog(Context context, Runnable onTransactionAdded) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bank_sync, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        SwitchCompat switchBankSync = view.findViewById(R.id.switch_bank_sync);
        LinearLayout layoutPermission = view.findViewById(R.id.layout_permission_status);
        ImageView ivPermissionIcon = view.findViewById(R.id.iv_permission_icon);
        TextView tvPermissionTitle = view.findViewById(R.id.tv_permission_title);
        TextView tvPermissionDesc = view.findViewById(R.id.tv_permission_desc);
        Button btnGrantPermission = view.findViewById(R.id.btn_grant_permission);
        android.widget.EditText etPasteSms = view.findViewById(R.id.et_paste_bank_sms);
        Button btnParseSms = view.findViewById(R.id.btn_parse_pasted_sms);
        Spinner spSample = view.findViewById(R.id.sp_sample_transaction);
        Button btnTest = view.findViewById(R.id.btn_test_bank_sync);
        Button btnClose = view.findViewById(R.id.btn_close_dialog);

        SharedPreferences prefs = context.getSharedPreferences(BankNotificationListenerService.PREF_NAME, Context.MODE_PRIVATE);
        boolean isSyncEnabled = prefs.getBoolean(BankNotificationListenerService.KEY_AUTO_SYNC_ENABLED, true);
        switchBankSync.setChecked(isSyncEnabled);

        switchBankSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(BankNotificationListenerService.KEY_AUTO_SYNC_ENABLED, isChecked).apply();
            Toast.makeText(
                    context,
                    isChecked ? "Đã bật tự động đọc thông báo ngân hàng" : "Đã tắt tự động đọc thông báo ngân hàng",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Cập nhật trạng thái quyền
        boolean hasPermission = isNotificationServiceEnabled(context);
        if (hasPermission) {
            layoutPermission.setBackgroundResource(R.drawable.bg_badge_income);
            ivPermissionIcon.setImageResource(R.drawable.ic_check_circle);
            ivPermissionIcon.setColorFilter(context.getResources().getColor(R.color.income_green));
            tvPermissionTitle.setText("Đang hoạt động (Đã cấp quyền đọc thông báo)");
            tvPermissionTitle.setTextColor(context.getResources().getColor(R.color.income_green));
            tvPermissionDesc.setText("Ví Thông Minh đang tự động lắng nghe và phân tích thông báo giao dịch.");
            btnGrantPermission.setVisibility(View.GONE);
        } else {
            layoutPermission.setBackgroundResource(R.drawable.bg_badge_warning);
            ivPermissionIcon.setImageResource(R.drawable.ic_warning);
            ivPermissionIcon.setColorFilter(context.getResources().getColor(R.color.warning_orange));
            tvPermissionTitle.setText("Chưa cấp quyền truy cập thông báo");
            tvPermissionTitle.setTextColor(context.getResources().getColor(R.color.warning_orange));
            btnGrantPermission.setVisibility(View.VISIBLE);
            btnGrantPermission.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                context.startActivity(intent);
                Toast.makeText(context, "Vui lòng tìm 'Ví Thông Minh' và bật cho phép truy cập thông báo", Toast.LENGTH_LONG).show();
            });
        }

        // Xử lý nút dán tin nhắn SMS / thông báo thủ công
        btnParseSms.setOnClickListener(v -> {
            String text = etPasteSms.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(context, "Vui lòng nhập hoặc dán nội dung tin nhắn/thông báo", Toast.LENGTH_SHORT).show();
                return;
            }

            com.example.quanlychitieu.service.BankNotificationParser.ParsedBankTransaction parsed =
                    com.example.quanlychitieu.service.BankNotificationParser.parse(null, "", text);

            if (parsed == null || parsed.amount <= 0) {
                Toast.makeText(context, "Không tìm thấy số tiền hợp lệ trong nội dung. Vui lòng kiểm tra lại!", Toast.LENGTH_LONG).show();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                com.example.quanlychitieu.model.PendingTransaction pending =
                        BankNotificationListenerService.saveToPendingAndNotifyUser(
                                context,
                                parsed.bankName,
                                parsed.type,
                                parsed.amount,
                                parsed.categoryName,
                                parsed.note,
                                text
                        );

                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        dialog.dismiss();
                        PendingTransactionDialogHelper.showApproveDialog(context, pending, onTransactionAdded);
                    });
                }
            });
        });

        // Cấu hình các mẫu giao dịch thử nghiệm
        List<String> sampleLabels = new ArrayList<>();
        sampleLabels.add("BIDV: -180.000 VNĐ (Ăn trưa cơm niêu)");
        sampleLabels.add("BIDV: +12.000.000 VNĐ (Lương công ty)");
        sampleLabels.add("MB Bank: -150.000 VNĐ (Ăn trưa Highlands Coffee)");
        sampleLabels.add("Vietcombank: +8.500.000 VNĐ (Lương tháng 10)");
        sampleLabels.add("Techcombank: -250.000 VNĐ (Mua sách Tiki)");
        sampleLabels.add("Ví MoMo: -45.000 VNĐ (The Coffee House)");
        sampleLabels.add("TPBank: -120.000 VNĐ (Đổ xăng xe máy)");
        sampleLabels.add("Ví ZaloPay: -35.000 VNĐ (Circle K)");

        ArrayAdapter<String> sampleAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, sampleLabels);
        spSample.setAdapter(sampleAdapter);

        btnTest.setOnClickListener(v -> {
            int pos = spSample.getSelectedItemPosition();
            Executors.newSingleThreadExecutor().execute(() -> {
                com.example.quanlychitieu.model.PendingTransaction pending;
                switch (pos) {
                    case 0:
                        pending = BankNotificationListenerService.processAndRecordBankTransaction(
                                context, "BIDV", "EXPENSE", 180000.0, "Ăn uống", "Ăn trưa cơm niêu"
                        );
                        break;
                    case 1:
                        pending = BankNotificationListenerService.processAndRecordBankTransaction(
                                context, "BIDV", "INCOME", 12000000.0, "Lương", "Lương công ty"
                        );
                        break;
                    case 2:
                        pending = BankNotificationListenerService.processAndRecordBankTransaction(
                                context, "MB Bank", "EXPENSE", 150000.0, "Ăn uống", "Ăn trưa Highlands Coffee"
                        );
                        break;
                    case 3:
                        pending = BankNotificationListenerService.processAndRecordBankTransaction(
                                context, "Vietcombank", "INCOME", 8500000.0, "Lương", "Lương tháng 10"
                        );
                        break;
                    case 4:
                        pending = BankNotificationListenerService.processAndRecordBankTransaction(
                                context, "Techcombank", "EXPENSE", 250000.0, "Mua sắm", "Mua sách Tiki"
                        );
                        break;
                    case 5:
                        pending = BankNotificationListenerService.processAndRecordBankTransaction(
                                context, "Ví MoMo", "EXPENSE", 45000.0, "Ăn uống", "The Coffee House"
                        );
                        break;
                    case 6:
                        pending = BankNotificationListenerService.processAndRecordBankTransaction(
                                context, "TPBank", "EXPENSE", 120000.0, "Đi lại", "Đổ xăng xe máy"
                        );
                        break;
                    case 7:
                    default:
                        pending = BankNotificationListenerService.processAndRecordBankTransaction(
                                context, "Ví ZaloPay", "EXPENSE", 35000.0, "Mua sắm", "Circle K"
                        );
                        break;
                }

                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(context, "Đã phát hiện giao dịch! Hãy chọn danh mục và duyệt vào ví.", Toast.LENGTH_SHORT).show();
                        PendingTransactionDialogHelper.showApproveDialog(context, pending, onTransactionAdded);
                    });
                }
            });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
