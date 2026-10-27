package com.example.quanlychitieu.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.PendingTransactionAdapter;
import com.example.quanlychitieu.database.AppDatabase;
import com.example.quanlychitieu.model.PendingTransaction;
import com.example.quanlychitieu.service.BankNotificationListenerService;
import com.example.quanlychitieu.service.BankNotificationParser;
import com.example.quanlychitieu.ui.dialog.BankSyncDialogHelper;
import com.example.quanlychitieu.ui.dialog.PendingTransactionDialogHelper;
import com.example.quanlychitieu.utils.ThemeHelper;

import java.util.List;
import java.util.concurrent.Executors;

public class PendingApprovalActivity extends AppCompatActivity implements PendingTransactionAdapter.OnPendingActionListener {

    private AppDatabase db;
    private PendingTransactionAdapter adapter;

    private LinearLayout layoutServiceStatus;
    private ImageView ivStatusIndicator;
    private TextView tvStatusTitle, tvStatusDesc, tvPendingCountHeader, btnClearAll;
    private Button btnActionService, btnQuickScanSms;
    private EditText etQuickPasteSms;
    private RecyclerView rvPending;
    private LinearLayout layoutEmpty;
    private TextView btnCreateSample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        db = AppDatabase.getInstance(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        observePendingTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatusUi();
    }

    private void initViews() {
        findViewById(R.id.btn_back_pending).setOnClickListener(v -> finish());

        layoutServiceStatus = findViewById(R.id.layout_service_status);
        ivStatusIndicator = findViewById(R.id.iv_status_indicator);
        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvStatusDesc = findViewById(R.id.tv_status_desc);
        btnActionService = findViewById(R.id.btn_action_service);

        etQuickPasteSms = findViewById(R.id.et_quick_paste_sms);
        btnQuickScanSms = findViewById(R.id.btn_quick_scan_sms);

        tvPendingCountHeader = findViewById(R.id.tv_pending_count_header);
        btnClearAll = findViewById(R.id.btn_clear_all_pending);
        btnCreateSample = findViewById(R.id.btn_create_sample_pending);

        rvPending = findViewById(R.id.rv_pending_approval_list);
        layoutEmpty = findViewById(R.id.layout_empty_pending_view);
    }

    private void setupRecyclerView() {
        adapter = new PendingTransactionAdapter(this, this);
        rvPending.setLayoutManager(new LinearLayoutManager(this));
        rvPending.setAdapter(adapter);
    }

    private void setupListeners() {
        // Nút Quét & Thêm nhanh tin nhắn SMS
        btnQuickScanSms.setOnClickListener(v -> {
            String text = etQuickPasteSms.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "Vui lòng nhập hoặc dán nội dung tin nhắn/thông báo", Toast.LENGTH_SHORT).show();
                return;
            }

            BankNotificationParser.ParsedBankTransaction parsed = BankNotificationParser.parse(null, "", text);
            if (parsed == null || parsed.amount <= 0) {
                Toast.makeText(this, "Không tìm thấy số tiền hợp lệ trong nội dung. Vui lòng kiểm tra lại!", Toast.LENGTH_LONG).show();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                BankNotificationListenerService.saveToPendingAndNotifyUser(
                        this,
                        parsed.bankName,
                        parsed.type,
                        parsed.amount,
                        parsed.categoryName,
                        parsed.note,
                        text
                );

                runOnUiThread(() -> {
                    etQuickPasteSms.setText("");
                    Toast.makeText(this, "✅ Đã thêm giao dịch vào danh sách chờ duyệt!", Toast.LENGTH_SHORT).show();
                });
            });
        });

        // Nút Thử tạo giao dịch mẫu
        btnCreateSample.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                BankNotificationListenerService.processAndRecordBankTransaction(
                        this,
                        "MB Bank",
                        "EXPENSE",
                        120000.0,
                        "Ăn uống",
                        "Highlands Coffee - 120.000 VNĐ"
                );

                runOnUiThread(() -> {
                    Toast.makeText(this, "⚡ Đã tạo giao dịch mẫu MB Bank!", Toast.LENGTH_SHORT).show();
                });
            });
        });

        // Nút Xóa hết giao dịch chờ duyệt
        btnClearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa tất cả")
                    .setMessage("Bạn có chắc chắn muốn xóa toàn bộ danh sách giao dịch đang chờ duyệt không?")
                    .setPositiveButton("Xóa hết", (d, which) -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            db.pendingTransactionDao().deleteAllPending();
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Đã xóa toàn bộ giao dịch chờ duyệt", Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Nút Cấp quyền / Kết nối lại dịch vụ đọc thông báo
        btnActionService.setOnClickListener(v -> {
            boolean hasPermission = BankSyncDialogHelper.isNotificationServiceEnabled(this);
            if (!hasPermission) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "Vui lòng tìm 'Ví Thông Minh' và bật cho phép truy cập thông báo", Toast.LENGTH_LONG).show();
            } else {
                rebindNotificationListenerService();
            }
        });
    }

    private void rebindNotificationListenerService() {
        try {
            ComponentName cn = new ComponentName(this, BankNotificationListenerService.class);
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NotificationListenerService.requestRebind(cn);
            }
            Toast.makeText(this, "🔄 Đã kích hoạt và làm mới kết nối Dịch vụ đọc thông báo!", Toast.LENGTH_SHORT).show();
            updateServiceStatusUi();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi kết nối lại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateServiceStatusUi() {
        boolean hasPermission = BankSyncDialogHelper.isNotificationServiceEnabled(this);
        if (hasPermission) {
            layoutServiceStatus.setBackgroundResource(R.drawable.bg_badge_income);
            ivStatusIndicator.setImageResource(R.drawable.ic_check_circle);
            ivStatusIndicator.setColorFilter(ContextCompat.getColor(this, R.color.income_green));
            tvStatusTitle.setText("Dịch vụ đọc thông báo đang hoạt động");
            tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.income_green));
            tvStatusDesc.setText("Hệ thống đang tự động lắng nghe biến động số dư từ các app ngân hàng & SMS.");
            btnActionService.setText("LÀM MỚI KẾT NỐI DỊCH VỤ");
        } else {
            layoutServiceStatus.setBackgroundResource(R.drawable.bg_badge_warning);
            ivStatusIndicator.setImageResource(R.drawable.ic_warning);
            ivStatusIndicator.setColorFilter(ContextCompat.getColor(this, R.color.warning_orange));
            tvStatusTitle.setText("Chưa cấp quyền truy cập thông báo");
            tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.warning_orange));
            tvStatusDesc.setText("Để tự động phát hiện giao dịch từ ngân hàng, bạn cần cấp quyền cho Ví Thông Minh.\n(Nếu nút cấp quyền bị mờ, vào Cài đặt máy > Ứng dụng > Ví Thông Minh > 3 chấm góc trên > Chọn 'Cho phép cài đặt bị hạn chế').");
            btnActionService.setText("CẤP QUYỀN TRUY CẬP THÔNG BÁO");
        }
    }

    private void observePendingTransactions() {
        db.pendingTransactionDao().getPendingTransactions().observe(this, list -> {
            if (list == null || list.isEmpty()) {
                rvPending.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                btnClearAll.setVisibility(View.GONE);
                tvPendingCountHeader.setText("Giao dịch chờ duyệt (0)");
            } else {
                rvPending.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
                btnClearAll.setVisibility(View.VISIBLE);
                tvPendingCountHeader.setText("Giao dịch chờ duyệt (" + list.size() + ")");
                adapter.setTransactions(list);
            }
        });
    }

    @Override
    public void onApprove(PendingTransaction item) {
        PendingTransactionDialogHelper.showApproveDialog(this, item, () -> {
            // Danh sách tự cập nhật qua LiveData
        });
    }

    @Override
    public void onDismiss(PendingTransaction item) {
        new AlertDialog.Builder(this)
                .setTitle("Bỏ qua giao dịch")
                .setMessage("Bạn có chắc chắn muốn bỏ qua giao dịch này không?")
                .setPositiveButton("Bỏ qua", (d, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.pendingTransactionDao().deleteById(item.getId());
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Đã bỏ qua giao dịch", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
