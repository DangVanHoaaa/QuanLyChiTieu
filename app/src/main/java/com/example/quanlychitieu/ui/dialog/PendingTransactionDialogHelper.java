package com.example.quanlychitieu.ui.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.database.AppDatabase;
import com.example.quanlychitieu.model.Category;
import com.example.quanlychitieu.model.PendingTransaction;
import com.example.quanlychitieu.service.BankNotificationListenerService;
import com.example.quanlychitieu.utils.CurrencyTextWatcher;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class PendingTransactionDialogHelper {

    public static void showApproveDialog(Context context, PendingTransaction pending, Runnable onComplete) {
        if (pending == null) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_approve_transaction, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.94),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvBankBadge = view.findViewById(R.id.tv_approve_bank_badge);
        TextView tvTypeBadge = view.findViewById(R.id.tv_approve_type_badge);
        TextView tvRawContent = view.findViewById(R.id.tv_approve_raw_content);
        EditText etAmount = view.findViewById(R.id.et_approve_amount);
        Spinner spCategory = view.findViewById(R.id.sp_approve_category);
        TextView btnAddCat = view.findViewById(R.id.btn_approve_add_cat);
        EditText etDate = view.findViewById(R.id.et_approve_date);
        EditText etNote = view.findViewById(R.id.et_approve_note);
        Button btnDismiss = view.findViewById(R.id.btn_dismiss_pending);
        Button btnConfirm = view.findViewById(R.id.btn_confirm_approve);

        // Hiển thị thông tin ngân hàng & loại giao dịch (cho phép chạm để chuyển đổi Thu nhập <-> Chi tiêu)
        tvBankBadge.setText("Ngân hàng: " + pending.getBankName());
        final boolean[] isIncomeHolder = {"INCOME".equalsIgnoreCase(pending.getType())};
        Runnable updateTypeUi = () -> {
            if (isIncomeHolder[0]) {
                tvTypeBadge.setText("Thu nhập 🔄");
                tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_income);
                tvTypeBadge.setTextColor(context.getResources().getColor(R.color.income_green));
            } else {
                tvTypeBadge.setText("Chi tiêu 🔄");
                tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_expense);
                tvTypeBadge.setTextColor(context.getResources().getColor(R.color.expense_red));
            }
        };
        updateTypeUi.run();

        tvRawContent.setText(pending.getRawContent() != null ? pending.getRawContent() : pending.getNote());

        // Định dạng số tiền
        etAmount.addTextChangedListener(new CurrencyTextWatcher(etAmount));
        etAmount.setText(String.valueOf((long) pending.getAmount()));

        // Ghi chú
        etNote.setText(pending.getNote());

        // Ngày giao dịch
        final String[] selectedDateIso = {pending.getDate() != null ? pending.getDate() : DateUtils.getTodayIso()};
        etDate.setText(DateUtils.formatDisplayDate(selectedDateIso[0]));
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(context, (dp, year, month, dayOfMonth) -> {
                c.set(year, month, dayOfMonth);
                selectedDateIso[0] = DateUtils.ISO_DATE_FORMAT.format(c.getTime());
                etDate.setText(DateUtils.formatDisplayDate(selectedDateIso[0]));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Tải danh sách Danh mục theo loại
        final List<Category> categoryList = new ArrayList<>();
        final List<String> categoryNames = new ArrayList<>();
        final ArrayAdapter<String> catAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, categoryNames);
        spCategory.setAdapter(catAdapter);

        Runnable loadCategories = () -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                List<Category> cats = db.categoryDao().getCategoriesByTypeSync(pending.getType());
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        categoryList.clear();
                        categoryNames.clear();
                        int selectedIndex = 0;
                        if (cats != null) {
                            categoryList.addAll(cats);
                            for (int i = 0; i < cats.size(); i++) {
                                Category c = cats.get(i);
                                categoryNames.add(c.getName());
                                if (c.getName().equalsIgnoreCase(pending.getSuggestedCategory())) {
                                    selectedIndex = i;
                                }
                            }
                        }
                        catAdapter.notifyDataSetChanged();
                        spCategory.setSelection(selectedIndex);
                    });
                }
            });
        };
        loadCategories.run();

        // Cho phép chạm vào huy hiệu loại để chuyển đổi qua lại giữa Thu nhập và Chi tiêu
        tvTypeBadge.setClickable(true);
        tvTypeBadge.setFocusable(true);
        tvTypeBadge.setOnClickListener(v -> {
            isIncomeHolder[0] = !isIncomeHolder[0];
            pending.setType(isIncomeHolder[0] ? "INCOME" : "EXPENSE");
            updateTypeUi.run();
            loadCategories.run();
            Toast.makeText(context, "Đã chuyển loại sang: " + (isIncomeHolder[0] ? "Thu nhập" : "Chi tiêu"), Toast.LENGTH_SHORT).show();
        });

        // Nút thêm danh mục mới
        btnAddCat.setOnClickListener(v -> {
            com.example.quanlychitieu.repository.ExpenseRepository repo =
                    new com.example.quanlychitieu.repository.ExpenseRepository((android.app.Application) context.getApplicationContext());
            TransactionDialogHelper.showAddCategoryDialog(context, repo, isIncomeHolder[0], loadCategories);
        });

        // Nút BỎ QUA
        btnDismiss.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getInstance(context).pendingTransactionDao().deleteById(pending.getId());
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Đã bỏ qua giao dịch", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        if (onComplete != null) onComplete.run();
                    });
                }
            });
        });

        // Nút DUYỆT VÀO VÍ
        btnConfirm.setOnClickListener(v -> {
            double finalAmount = CurrencyUtils.parseAmount(etAmount.getText().toString());
            if (finalAmount <= 0) {
                Toast.makeText(context, "Vui lòng nhập số tiền lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }

            int catPos = spCategory.getSelectedItemPosition();
            if (catPos < 0 || catPos >= categoryList.size()) {
                Toast.makeText(context, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
                return;
            }

            Category selectedCategory = categoryList.get(catPos);
            String inputNote = etNote.getText().toString().trim();
            final String finalNote = inputNote.isEmpty()
                    ? pending.getBankName() + ": Giao dịch " + selectedCategory.getName()
                    : inputNote;

            Executors.newSingleThreadExecutor().execute(() -> {
                BankNotificationListenerService.approveAndSaveTransaction(
                        context,
                        pending,
                        selectedCategory.getId(),
                        selectedCategory.getName(),
                        finalNote,
                        finalAmount,
                        selectedDateIso[0]
                );

                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "🎉 Đã duyệt giao dịch vào ví thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        if (onComplete != null) onComplete.run();
                    });
                }
            });
        });

        dialog.show();
    }
}
