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

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.BudgetAdapter;
import com.example.quanlychitieu.model.Budget;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.ui.dialog.BudgetDialogHelper;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class BudgetFragment extends Fragment implements BudgetAdapter.OnBudgetActionListener {

    private ExpenseRepository repository;
    private BudgetAdapter adapter;

    private TextView tvMonth;
    private TextView tvTotalAmount;
    private ProgressBar progressTotal;
    private TextView tvTotalSpent;
    private TextView tvTotalRemaining;
    private TextView tvTotalAlert;
    private MaterialButton btnSetTotalBudget;

    private MaterialButton btnAddCategoryBudget;
    private RecyclerView rvCategoryBudgets;
    private LinearLayout layoutEmptyCategory;
    private android.widget.EditText etSearchBudget;
    private com.google.android.material.chip.Chip chipSortBudget;

    private List<BudgetAdapter.BudgetItemModel> allCategoryModels = new ArrayList<>();
    private String searchKeyword = "";
    private int sortMode = 0; // 0: Tiền cao -> thấp, 1: Chi nhiều nhất, 2: Tên A-Z

    private String currentMonth;
    private Budget currentTotalBudget;
    private double currentMonthSpent = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        repository = new ExpenseRepository(requireActivity().getApplication());
        currentMonth = DateUtils.getCurrentMonth();

        tvMonth = view.findViewById(R.id.tv_budget_month);
        tvTotalAmount = view.findViewById(R.id.tv_total_budget_amount);
        progressTotal = view.findViewById(R.id.progress_total_budget);
        tvTotalSpent = view.findViewById(R.id.tv_total_budget_spent);
        tvTotalRemaining = view.findViewById(R.id.tv_total_budget_remaining);
        tvTotalAlert = view.findViewById(R.id.tv_total_budget_alert);
        btnSetTotalBudget = view.findViewById(R.id.btn_set_total_budget);

        btnAddCategoryBudget = view.findViewById(R.id.btn_add_category_budget);
        rvCategoryBudgets = view.findViewById(R.id.rv_category_budgets);
        layoutEmptyCategory = view.findViewById(R.id.layout_empty_category_budget);
        etSearchBudget = view.findViewById(R.id.et_search_budget);
        chipSortBudget = view.findViewById(R.id.chip_sort_budget);

        tvMonth.setText(DateUtils.formatDisplayMonth(currentMonth));

        setupRecyclerView();
        setupListeners();
        observeBudgetData();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new BudgetAdapter(requireContext(), this);
        rvCategoryBudgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategoryBudgets.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSetTotalBudget.setOnClickListener(v -> {
            BudgetDialogHelper.showSetBudgetDialog(
                    requireContext(),
                    repository,
                    currentMonth,
                    currentTotalBudget,
                    false,
                    null
            );
        });

        btnAddCategoryBudget.setOnClickListener(v -> {
            BudgetDialogHelper.showSetBudgetDialog(
                    requireContext(),
                    repository,
                    currentMonth,
                    null,
                    true,
                    null
            );
        });

        if (etSearchBudget != null) {
            etSearchBudget.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchKeyword = s != null ? s.toString().trim() : "";
                    filterAndSortCategoryBudgets();
                }
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (chipSortBudget != null) {
            chipSortBudget.setOnClickListener(v -> {
                sortMode = (sortMode + 1) % 3;
                if (sortMode == 0) {
                    chipSortBudget.setText("Tiền ↓");
                } else if (sortMode == 1) {
                    chipSortBudget.setText("Đã chi ↓");
                } else {
                    chipSortBudget.setText("Tên A-Z");
                }
                filterAndSortCategoryBudgets();
            });
        }
    }

    private void observeBudgetData() {
        // Chi tiêu tổng của tháng
        repository.getMonthlyExpense(currentMonth).observe(getViewLifecycleOwner(), spent -> {
            currentMonthSpent = spent != null ? spent : 0.0;
            updateTotalBudgetUi();
        });

        // Ngân sách tổng của tháng
        repository.getTotalBudgetByMonth(currentMonth).observe(getViewLifecycleOwner(), budget -> {
            currentTotalBudget = budget;
            updateTotalBudgetUi();
        });

        // Ngân sách của các danh mục trong tháng
        repository.getBudgetsByMonth(currentMonth).observe(getViewLifecycleOwner(), budgets -> {
            List<Budget> categoryBudgets = new ArrayList<>();
            if (budgets != null) {
                for (Budget b : budgets) {
                    if (b.getCategoryId() != 0) {
                        categoryBudgets.add(b);
                    }
                }
            }

            if (categoryBudgets.isEmpty()) {
                rvCategoryBudgets.setVisibility(View.GONE);
                layoutEmptyCategory.setVisibility(View.VISIBLE);
            } else {
                rvCategoryBudgets.setVisibility(View.VISIBLE);
                layoutEmptyCategory.setVisibility(View.GONE);
                loadCategoryExpenses(categoryBudgets);
            }
        });
    }

    private void updateTotalBudgetUi() {
        if (currentTotalBudget == null || currentTotalBudget.getAmount() <= 0) {
            tvTotalAmount.setText("Chưa thiết lập");
            btnSetTotalBudget.setText("Thiết lập");
            progressTotal.setProgress(0);
            tvTotalSpent.setText("Đã chi: " + CurrencyUtils.formatVnd(currentMonthSpent));
            tvTotalRemaining.setText("Chưa thiết lập ngân sách");
            tvTotalAlert.setVisibility(View.GONE);
            return;
        }

        btnSetTotalBudget.setText("Sửa");
        double total = currentTotalBudget.getAmount();
        double remaining = total - currentMonthSpent;
        int percent = (int) Math.round((currentMonthSpent / total) * 100);

        tvTotalAmount.setText(CurrencyUtils.formatVnd(total));
        progressTotal.setProgress(Math.min(percent, 100));
        tvTotalSpent.setText("Đã chi: " + CurrencyUtils.formatVnd(currentMonthSpent));

        if (remaining >= 0) {
            tvTotalRemaining.setText("Còn lại: " + CurrencyUtils.formatVnd(remaining));
            tvTotalRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark));
        } else {
            tvTotalRemaining.setText("Vượt mức: " + CurrencyUtils.formatVnd(Math.abs(remaining)));
            tvTotalRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        }

        // Cảnh báo ngân sách
        if (currentMonthSpent > total) {
            tvTotalAlert.setVisibility(View.VISIBLE);
            tvTotalAlert.setText(R.string.warning_budget_over);
            tvTotalAlert.setBackgroundResource(R.drawable.bg_badge_expense);
            tvTotalAlert.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_red));
        } else if (percent >= 80) {
            tvTotalAlert.setVisibility(View.VISIBLE);
            tvTotalAlert.setText(R.string.warning_budget_near);
            tvTotalAlert.setBackgroundResource(R.drawable.bg_badge_warning);
            tvTotalAlert.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_orange));
        } else {
            tvTotalAlert.setVisibility(View.GONE);
        }
    }

    private void loadCategoryExpenses(List<Budget> categoryBudgets) {
        List<BudgetAdapter.BudgetItemModel> models = new ArrayList<>();
        int[] counter = {0};

        for (Budget b : categoryBudgets) {
            repository.getMonthlyExpenseForCategory(currentMonth, b.getCategoryId(), spent -> {
                models.add(new BudgetAdapter.BudgetItemModel(b, spent));
                counter[0]++;
                if (counter[0] == categoryBudgets.size()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            allCategoryModels = models;
                            filterAndSortCategoryBudgets();
                        });
                    }
                }
            });
        }
    }

    private void filterAndSortCategoryBudgets() {
        List<BudgetAdapter.BudgetItemModel> filtered = new ArrayList<>();
        for (BudgetAdapter.BudgetItemModel item : allCategoryModels) {
            String name = item.getBudget().getCategoryName();
            if (searchKeyword.isEmpty() || (name != null && name.toLowerCase().contains(searchKeyword.toLowerCase()))) {
                filtered.add(item);
            }
        }

        java.util.Collections.sort(filtered, (o1, o2) -> {
            if (sortMode == 0) {
                return Double.compare(o2.getBudget().getAmount(), o1.getBudget().getAmount());
            } else if (sortMode == 1) {
                return Double.compare(o2.getSpentAmount(), o1.getSpentAmount());
            } else {
                String n1 = o1.getBudget().getCategoryName() != null ? o1.getBudget().getCategoryName() : "";
                String n2 = o2.getBudget().getCategoryName() != null ? o2.getBudget().getCategoryName() : "";
                return n1.compareToIgnoreCase(n2);
            }
        });

        adapter.setItems(filtered);
    }

    @Override
    public void onEdit(Budget budget) {
        BudgetDialogHelper.showSetBudgetDialog(
                requireContext(),
                repository,
                currentMonth,
                budget,
                true,
                null
        );
    }

    @Override
    public void onDelete(Budget budget) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage("Bạn có chắc muốn xóa ngân sách danh mục này?")
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    repository.deleteBudget(budget, res -> {
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
