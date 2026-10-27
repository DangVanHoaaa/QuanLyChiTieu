package com.example.quanlychitieu.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.SavingsGoal;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SavingsDialogHelper {

    public interface OnSavingsSavedListener {
        void onSaved();
    }

    public static void showAddEditDialog(
            Context context,
            ExpenseRepository repository,
            SavingsGoal goalToEdit,
            OnSavingsSavedListener listener
    ) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_edit_savings, null);

        TextView tvTitle = view.findViewById(R.id.tv_dialog_savings_title);
        TextInputEditText etName = view.findViewById(R.id.et_savings_name);
        TextInputEditText etTarget = view.findViewById(R.id.et_savings_target);
        TextInputEditText etInitial = view.findViewById(R.id.et_savings_initial);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel_savings);
        MaterialButton btnSave = view.findViewById(R.id.btn_save_savings);

        etTarget.addTextChangedListener(new com.example.quanlychitieu.utils.CurrencyTextWatcher(etTarget));
        etInitial.addTextChangedListener(new com.example.quanlychitieu.utils.CurrencyTextWatcher(etInitial));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final boolean isEdit = (goalToEdit != null);
        tvTitle.setText(isEdit ? R.string.edit_savings_title : R.string.add_savings_title);

        if (isEdit) {
            etName.setText(goalToEdit.getName());
            etTarget.setText(String.valueOf(Math.round(goalToEdit.getTargetAmount())));
            etInitial.setText(String.valueOf(Math.round(goalToEdit.getCurrentAmount())));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            double target = CurrencyUtils.parseAmount(etTarget.getText() != null ? etTarget.getText().toString() : "");
            double initial = CurrencyUtils.parseAmount(etInitial.getText() != null ? etInitial.getText().toString() : "");

            if (name.isEmpty()) {
                Toast.makeText(context, R.string.err_empty_name, Toast.LENGTH_SHORT).show();
                etName.requestFocus();
                return;
            }

            if (target <= 0) {
                Toast.makeText(context, R.string.err_invalid_amount, Toast.LENGTH_SHORT).show();
                etTarget.requestFocus();
                return;
            }

            if (isEdit) {
                goalToEdit.setName(name);
                goalToEdit.setTargetAmount(target);
                goalToEdit.setCurrentAmount(initial);
                goalToEdit.setCompleted(initial >= target);

                repository.updateSavingsGoal(goalToEdit, res -> {
                    dialog.dismiss();
                    if (listener != null) listener.onSaved();
                });
            } else {
                SavingsGoal newGoal = new SavingsGoal(
                        name,
                        target,
                        initial,
                        DateUtils.getTodayIso(),
                        initial >= target
                );

                repository.insertSavingsGoal(newGoal, res -> {
                    dialog.dismiss();
                    if (listener != null) listener.onSaved();
                });
            }

            Toast.makeText(context, R.string.msg_saved_success, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    public static void showDepositDialog(
            Context context,
            ExpenseRepository repository,
            SavingsGoal goal,
            OnSavingsSavedListener listener
    ) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_deposit_savings, null);

        TextView tvGoalName = view.findViewById(R.id.tv_deposit_goal_name);
        TextInputEditText etAmount = view.findViewById(R.id.et_deposit_amount);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel_deposit);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm_deposit);

        etAmount.addTextChangedListener(new com.example.quanlychitieu.utils.CurrencyTextWatcher(etAmount));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        tvGoalName.setText("Mục tiêu: " + goal.getName());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            double addAmount = CurrencyUtils.parseAmount(amountStr);

            if (addAmount <= 0) {
                Toast.makeText(context, R.string.err_invalid_amount, Toast.LENGTH_SHORT).show();
                etAmount.requestFocus();
                return;
            }

            double newCurrent = goal.getCurrentAmount() + addAmount;
            goal.setCurrentAmount(newCurrent);
            boolean wasCompletedBefore = goal.isCompleted();
            boolean isCompletedNow = newCurrent >= goal.getTargetAmount();
            goal.setCompleted(isCompletedNow);

            repository.updateSavingsGoal(goal, res -> {
                dialog.dismiss();
                if (listener != null) listener.onSaved();
            });

            if (!wasCompletedBefore && isCompletedNow) {
                Toast.makeText(context, R.string.msg_savings_congrats, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Đã nạp thêm " + CurrencyUtils.formatVnd(addAmount) + " vào mục tiêu!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
