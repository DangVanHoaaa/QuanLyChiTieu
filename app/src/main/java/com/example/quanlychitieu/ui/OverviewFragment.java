package com.example.quanlychitieu.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.MainActivity;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.TransactionAdapter;
import com.example.quanlychitieu.model.Budget;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.ui.dialog.TransactionDialogHelper;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

public class OverviewFragment extends Fragment implements TransactionAdapter.OnTransactionActionListener {

    private ExpenseRepository repository;
    private TransactionAdapter adapter;

    private TextView tvCurrentDate;
    private TextView tvTotalBalance;
    private TextView tvMonthlyIncome;
    private TextView tvMonthlyExpense;

    private TextView tvBudgetPercent;
    private ProgressBar progressBudget;
    private TextView tvBudgetSpentText;
    private TextView tvBudgetRemainingText;
    private TextView tvBudgetWarning;

    private RecyclerView rvRecent;
    private LinearLayout layoutEmpty;
    private ExtendedFloatingActionButton fabAdd;
    private TextView btnViewAll;

    private double totalIncomeAllTime = 0.0;
    private double totalExpenseAllTime = 0.0;
    private double currentMonthSpent = 0.0;
    private double currentMonthBudgetAmount = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        repository = new ExpenseRepository(requireActivity().getApplication());

        tvCurrentDate = view.findViewById(R.id.tv_current_date);
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvMonthlyIncome = view.findViewById(R.id.tv_overview_income);
        tvMonthlyExpense = view.findViewById(R.id.tv_overview_expense);

        tvBudgetPercent = view.findViewById(R.id.tv_budget_percent);
        progressBudget = view.findViewById(R.id.progress_budget);
        tvBudgetSpentText = view.findViewById(R.id.tv_budget_spent_text);
        tvBudgetRemainingText = view.findViewById(R.id.tv_budget_remaining_text);
        tvBudgetWarning = view.findViewById(R.id.tv_budget_warning);

        rvRecent = view.findViewById(R.id.rv_recent_transactions);
        layoutEmpty = view.findViewById(R.id.layout_empty_recent);
        fabAdd = view.findViewById(R.id.fab_add_transaction);
        btnViewAll = view.findViewById(R.id.btn_view_all_transactions);
        View btnOpenAi = view.findViewById(R.id.btn_open_ai_chatbot);
        if (btnOpenAi != null) {
            btnOpenAi.setOnClickListener(v -> {
                startActivity(new android.content.Intent(requireContext(), ChatBotActivity.class));
            });
        }
        View btnTheme = view.findViewById(R.id.btn_change_theme);
        if (btnTheme != null) {
            btnTheme.setOnClickListener(v -> {
                com.example.quanlychitieu.utils.ThemeHelper.showThemeDialog(requireActivity(), () -> {
                    requireActivity().recreate();
                });
            });
        }

        setupRecyclerView();
        setupListeners();
        observeData();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(requireContext(), this);
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecent.setAdapter(adapter);
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            TransactionDialogHelper.showAddEditDialog(requireContext(), repository, null, () -> {
                // Tự động cập nhật qua LiveData
            });
        });

        btnViewAll.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTransactions();
            }
        });
    }

    private void observeData() {
        String currentMonth = DateUtils.getCurrentMonth();
        tvCurrentDate.setText(DateUtils.formatDisplayMonth(currentMonth));

        // 1. Tính tổng số dư tích lũy: Tổng thu nhập tích lũy - Tổng chi tiêu tích lũy
        repository.getTotalIncome().observe(getViewLifecycleOwner(), income -> {
            totalIncomeAllTime = income != null ? income : 0.0;
            updateBalance();
        });

        repository.getTotalExpense().observe(getViewLifecycleOwner(), expense -> {
            totalExpenseAllTime = expense != null ? expense : 0.0;
            updateBalance();
        });

        // 2. Thu nhập và Chi tiêu của tháng hiện tại
        repository.getMonthlyIncome(currentMonth).observe(getViewLifecycleOwner(), income -> {
            double inc = income != null ? income : 0.0;
            tvMonthlyIncome.setText(CurrencyUtils.formatVnd(inc));
        });

        repository.getMonthlyExpense(currentMonth).observe(getViewLifecycleOwner(), expense -> {
            currentMonthSpent = expense != null ? expense : 0.0;
            tvMonthlyExpense.setText(CurrencyUtils.formatVnd(currentMonthSpent));
            updateBudgetProgress();
        });

        // 3. Ngân sách tháng hiện tại
        repository.getTotalBudgetByMonth(currentMonth).observe(getViewLifecycleOwner(), budget -> {
            currentMonthBudgetAmount = budget != null ? budget.getAmount() : 0.0;
            updateBudgetProgress();
        });

        // 4. Giao dịch gần đây (tối đa 5 giao dịch)
        repository.getRecentTransactions(5).observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) {
                rvRecent.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            } else {
                rvRecent.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
                adapter.setTransactions(list);
            }
        });
    }

    private void updateBalance() {
        double balance = totalIncomeAllTime - totalExpenseAllTime;
        tvTotalBalance.setText(CurrencyUtils.formatVnd(balance));
    }

    private void updateBudgetProgress() {
        if (currentMonthBudgetAmount <= 0) {
            tvBudgetPercent.setText("Chưa đặt");
            progressBudget.setProgress(0);
            tvBudgetSpentText.setText("Đã chi: " + CurrencyUtils.formatVnd(currentMonthSpent));
            tvBudgetRemainingText.setText("Chưa thiết lập ngân sách");
            tvBudgetWarning.setVisibility(View.GONE);
            return;
        }

        double remaining = currentMonthBudgetAmount - currentMonthSpent;
        int percent = (int) Math.round((currentMonthSpent / currentMonthBudgetAmount) * 100);

        tvBudgetPercent.setText(percent + "%");
        progressBudget.setProgress(Math.min(percent, 100));
        tvBudgetSpentText.setText("Đã chi: " + CurrencyUtils.formatVnd(currentMonthSpent));

        if (remaining >= 0) {
            tvBudgetRemainingText.setText("Còn lại: " + CurrencyUtils.formatVnd(remaining));
            tvBudgetRemainingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark));
        } else {
            tvBudgetRemainingText.setText("Vượt: " + CurrencyUtils.formatVnd(Math.abs(remaining)));
            tvBudgetRemainingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        }

        // Cảnh báo ngân sách
        if (currentMonthSpent > currentMonthBudgetAmount) {
            tvBudgetWarning.setVisibility(View.VISIBLE);
            tvBudgetWarning.setText(R.string.warning_budget_over);
            tvBudgetWarning.setBackgroundResource(R.drawable.bg_badge_expense);
            tvBudgetWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_red));
        } else if (percent >= 80) {
            tvBudgetWarning.setVisibility(View.VISIBLE);
            tvBudgetWarning.setText(R.string.warning_budget_near);
            tvBudgetWarning.setBackgroundResource(R.drawable.bg_badge_warning);
            tvBudgetWarning.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_orange));
        } else {
            tvBudgetWarning.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEdit(Transaction transaction) {
        TransactionDialogHelper.showAddEditDialog(requireContext(), repository, transaction, null);
    }

    @Override
    public void onDelete(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_msg)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    repository.deleteTransaction(transaction, res -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), R.string.msg_deleted_success, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
