package com.example.quanlychitieu.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.Budget;
import com.example.quanlychitieu.model.Category;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class BudgetDialogHelper {

    public interface OnBudgetSavedListener {
        void onSaved();
    }

    public static void showSetBudgetDialog(
            Context context,
            ExpenseRepository repository,
            String month,
            Budget budgetToEdit,
            boolean isCategoryBudget,
            OnBudgetSavedListener listener
    ) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_set_budget, null);

        TextView tvTitle = view.findViewById(R.id.tv_dialog_budget_title);
        TextView tvLabelCategory = view.findViewById(R.id.tv_label_budget_category);
        Spinner spCategory = view.findViewById(R.id.sp_budget_category);
        TextInputEditText etAmount = view.findViewById(R.id.et_budget_amount);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel_budget);
        MaterialButton btnSave = view.findViewById(R.id.btn_save_budget);

        // Định dạng số tiền phân cách hàng nghìn theo thời gian thực
        etAmount.addTextChangedListener(new com.example.quanlychitieu.utils.CurrencyTextWatcher(etAmount));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final boolean isEdit = (budgetToEdit != null);

        if (!isCategoryBudget) {
            tvTitle.setText(isEdit ? "Sửa ngân sách tổng tháng" : "Thiết lập ngân sách tổng tháng");
            tvLabelCategory.setVisibility(View.GONE);
            spCategory.setVisibility(View.GONE);
            if (isEdit) {
                etAmount.setText(String.valueOf(Math.round(budgetToEdit.getAmount())));
            }
        } else {
            tvTitle.setText(isEdit ? "Sửa ngân sách danh mục" : "Thêm ngân sách danh mục");
            tvLabelCategory.setVisibility(View.VISIBLE);
            spCategory.setVisibility(View.VISIBLE);
            if (isEdit) {
                etAmount.setText(String.valueOf(Math.round(budgetToEdit.getAmount())));
            }
        }

        final List<Category> expenseCategories = new ArrayList<>();
        final ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_dropdown, new ArrayList<>());
        categoryAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spCategory.setAdapter(categoryAdapter);

        if (isCategoryBudget) {
            repository.getCategoriesByTypeSync("EXPENSE", categories -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    expenseCategories.clear();
                    if (categories != null) {
                        expenseCategories.addAll(categories);
                    }
                    List<String> names = new ArrayList<>();
                    int selectedIndex = 0;
                    for (int i = 0; i < expenseCategories.size(); i++) {
                        names.add(expenseCategories.get(i).getName());
                        if (isEdit && expenseCategories.get(i).getId() == budgetToEdit.getCategoryId()) {
                            selectedIndex = i;
                        }
                    }
                    categoryAdapter.clear();
                    categoryAdapter.addAll(names);
                    categoryAdapter.notifyDataSetChanged();
                    if (!expenseCategories.isEmpty()) {
                        spCategory.setSelection(selectedIndex);
                    }
                });
            });
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            double amount = CurrencyUtils.parseAmount(amountStr);

            if (amount <= 0) {
                Toast.makeText(context, R.string.err_invalid_amount, Toast.LENGTH_SHORT).show();
                return;
            }

            int categoryId = 0;
            String categoryName = "Ngân sách tổng";

            if (isCategoryBudget) {
                int pos = spCategory.getSelectedItemPosition();
                if (pos < 0 || pos >= expenseCategories.size()) {
                    Toast.makeText(context, R.string.err_empty_category, Toast.LENGTH_SHORT).show();
                    return;
                }
                Category cat = expenseCategories.get(pos);
                categoryId = cat.getId();
                categoryName = cat.getName();
            }

            Budget budget = isEdit ? budgetToEdit : new Budget(month, categoryId, categoryName, amount);
            budget.setAmount(amount);
            budget.setCategoryId(categoryId);
            budget.setCategoryName(categoryName);
            budget.setMonth(month);

            repository.saveOrUpdateBudget(budget, res -> {
                dialog.dismiss();
                if (listener != null) listener.onSaved();
            });

            Toast.makeText(context, R.string.msg_saved_success, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}
