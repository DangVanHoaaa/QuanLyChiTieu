package com.example.quanlychitieu.ui.dialog;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.Category;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TransactionDialogHelper {

    public interface OnTransactionSavedListener {
        void onSaved();
    }

    public static void showAddEditDialog(Context context, ExpenseRepository repository, Transaction transactionToEdit, OnTransactionSavedListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_edit_transaction, null);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        RadioGroup rgType = dialogView.findViewById(R.id.rg_transaction_type);
        RadioButton rbIncome = dialogView.findViewById(R.id.rb_income);
        RadioButton rbExpense = dialogView.findViewById(R.id.rb_expense);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        Spinner spCategory = dialogView.findViewById(R.id.sp_category);
        TextInputEditText etDate = dialogView.findViewById(R.id.et_date);
        TextInputEditText etNote = dialogView.findViewById(R.id.et_note);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // Định dạng số tiền phân cách hàng nghìn theo thời gian thực (ví dụ 1.000.000)
        etAmount.addTextChangedListener(new com.example.quanlychitieu.utils.CurrencyTextWatcher(etAmount));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final boolean isEditMode = (transactionToEdit != null);
        final String[] selectedDate = {isEditMode ? transactionToEdit.getDate() : DateUtils.getTodayIso()};

        tvTitle.setText(isEditMode ? R.string.edit_transaction_title : R.string.add_transaction_title);
        etDate.setText(DateUtils.formatDisplayDate(selectedDate[0]));

        if (isEditMode) {
            if ("INCOME".equalsIgnoreCase(transactionToEdit.getType())) {
                rbIncome.setChecked(true);
            } else {
                rbExpense.setChecked(true);
            }
            etAmount.setText(String.valueOf(Math.round(transactionToEdit.getAmount())));
            etNote.setText(transactionToEdit.getNote());
        }

        final List<Category> currentCategories = new ArrayList<>();
        final ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, R.layout.item_spinner_dropdown, new ArrayList<>());
        categoryAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spCategory.setAdapter(categoryAdapter);

        // Hàm nạp danh mục theo Loại (INCOME / EXPENSE)
        Runnable loadCategories = () -> {
            String type = rbIncome.isChecked() ? "INCOME" : "EXPENSE";
            repository.getCategoriesByTypeSync(type, categories -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    currentCategories.clear();
                    if (categories != null) {
                        currentCategories.addAll(categories);
                    }
                    List<String> names = new ArrayList<>();
                    int selectedIndex = 0;
                    for (int i = 0; i < currentCategories.size(); i++) {
                        names.add(currentCategories.get(i).getName());
                        if (isEditMode && currentCategories.get(i).getName().equals(transactionToEdit.getCategoryName())) {
                            selectedIndex = i;
                        }
                    }
                    categoryAdapter.clear();
                    categoryAdapter.addAll(names);
                    categoryAdapter.notifyDataSetChanged();
                    if (!currentCategories.isEmpty()) {
                        spCategory.setSelection(selectedIndex);
                    }
                });
            });
        };

        loadCategories.run();

        rgType.setOnCheckedChangeListener((group, checkedId) -> loadCategories.run());

        // Bấm nút thêm danh mục mới
        TextView btnAddCategory = dialogView.findViewById(R.id.btn_add_custom_category);
        if (btnAddCategory != null) {
            btnAddCategory.setOnClickListener(v -> {
                showAddCategoryDialog(context, repository, rbIncome.isChecked(), () -> {
                    loadCategories.run();
                });
            });
        }

        // Chọn ngày qua DatePickerDialog
        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try {
                cal.setTime(DateUtils.ISO_DATE_FORMAT.parse(selectedDate[0]));
            } catch (Exception ignored) {
            }

            DatePickerDialog datePicker = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(year, month, dayOfMonth);
                selectedDate[0] = DateUtils.ISO_DATE_FORMAT.format(chosen.getTime());
                etDate.setText(DateUtils.formatDisplayDate(selectedDate[0]));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            double amount = CurrencyUtils.parseAmount(amountStr);

            if (amount <= 0) {
                Toast.makeText(context, R.string.err_invalid_amount, Toast.LENGTH_SHORT).show();
                etAmount.requestFocus();
                return;
            }

            int catPos = spCategory.getSelectedItemPosition();
            if (catPos < 0 || catPos >= currentCategories.size()) {
                Toast.makeText(context, R.string.err_empty_category, Toast.LENGTH_SHORT).show();
                return;
            }

            Category selectedCategory = currentCategories.get(catPos);
            String type = rbIncome.isChecked() ? "INCOME" : "EXPENSE";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (isEditMode) {
                transactionToEdit.setType(type);
                transactionToEdit.setAmount(amount);
                transactionToEdit.setCategoryId(selectedCategory.getId());
                transactionToEdit.setCategoryName(selectedCategory.getName());
                transactionToEdit.setDate(selectedDate[0]);
                transactionToEdit.setNote(note);

                repository.updateTransaction(transactionToEdit, res -> {
                    dialog.dismiss();
                    if (listener != null) listener.onSaved();
                });
            } else {
                Transaction newTransaction = new Transaction(
                        type,
                        amount,
                        selectedCategory.getId(),
                        selectedCategory.getName(),
                        selectedDate[0],
                        note,
                        System.currentTimeMillis()
                );

                repository.insertTransaction(newTransaction, id -> {
                    dialog.dismiss();
                    if (listener != null) listener.onSaved();
                });
            }

            Toast.makeText(context, R.string.msg_saved_success, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    public static void showAddCategoryDialog(Context context, ExpenseRepository repository, boolean isIncomeDefault, Runnable onCategoryAdded) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null);
        RadioGroup rgType = view.findViewById(R.id.rg_new_cat_type);
        RadioButton rbExpense = view.findViewById(R.id.rb_cat_expense);
        RadioButton rbIncome = view.findViewById(R.id.rb_cat_income);
        TextInputEditText etName = view.findViewById(R.id.et_new_cat_name);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel_new_cat);
        MaterialButton btnSave = view.findViewById(R.id.btn_save_new_cat);

        if (isIncomeDefault) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }

        AlertDialog catDialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();

        if (catDialog.getWindow() != null) {
            catDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> catDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập tên danh mục!", Toast.LENGTH_SHORT).show();
                etName.requestFocus();
                return;
            }

            String type = rbIncome.isChecked() ? "INCOME" : "EXPENSE";
            String iconName = type.equals("INCOME") ? "ic_cat_income_other" : "ic_cat_expense_other";
            String colorHex = type.equals("INCOME") ? "#16A34A" : "#EF4444";

            repository.checkCategoryExists(name, type, exists -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (exists) {
                        Toast.makeText(context, "Danh mục '" + name + "' đã tồn tại!", Toast.LENGTH_SHORT).show();
                        etName.requestFocus();
                        return;
                    }

                    Category newCat = new Category(name, type, iconName, colorHex);
                    repository.insertCategory(newCat, id -> {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            catDialog.dismiss();
                            Toast.makeText(context, "Đã thêm danh mục: " + name, Toast.LENGTH_SHORT).show();
                            if (onCategoryAdded != null) {
                                onCategoryAdded.run();
                            }
                        });
                    });
                });
            });
        });

        catDialog.show();
    }
}
