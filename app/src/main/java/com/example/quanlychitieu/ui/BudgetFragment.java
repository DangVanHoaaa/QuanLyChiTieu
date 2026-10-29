package com.example.quanlychitieu.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.BudgetAdapter;
import com.example.quanlychitieu.adapter.BudgetHistoryAdapter;
import com.example.quanlychitieu.model.Budget;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.ui.dialog.BudgetDialogHelper;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BudgetFragment extends Fragment implements BudgetAdapter.OnBudgetActionListener {

    private ExpenseRepository repository;
    private BudgetAdapter adapter;

    private TextView tvMonth;
    private ImageButton btnPrevBudgetMonth, btnNextBudgetMonth;
    private View layoutSelectBudgetMonth;
    private View btnBudgetHistory;

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

    private Calendar selectedBudgetCalendar = Calendar.getInstance();
    private String currentMonth;
    private Budget currentTotalBudget;
    private double currentMonthSpent = 0;

    private LiveData<Double> liveMonthlySpent;
    private LiveData<Budget> liveTotalBudget;
    private LiveData<List<Budget>> liveCategoryBudgets;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        repository = new ExpenseRepository(requireActivity().getApplication());
        currentMonth = DateUtils.formatMonthIso(selectedBudgetCalendar);

        tvMonth = view.findViewById(R.id.tv_budget_month);
        btnPrevBudgetMonth = view.findViewById(R.id.btn_prev_budget_month);
        btnNextBudgetMonth = view.findViewById(R.id.btn_next_budget_month);
        layoutSelectBudgetMonth = view.findViewById(R.id.layout_select_budget_month);
        btnBudgetHistory = view.findViewById(R.id.btn_budget_history);

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
        if (btnPrevBudgetMonth != null) {
            btnPrevBudgetMonth.setOnClickListener(v -> {
                selectedBudgetCalendar.add(Calendar.MONTH, -1);
                observeBudgetData();
            });
        }

        if (btnNextBudgetMonth != null) {
            btnNextBudgetMonth.setOnClickListener(v -> {
                selectedBudgetCalendar.add(Calendar.MONTH, 1);
                observeBudgetData();
            });
        }

        if (layoutSelectBudgetMonth != null) {
            layoutSelectBudgetMonth.setOnClickListener(v -> {
                showSelectMonthDialog();
            });
        }

        if (btnBudgetHistory != null) {
            btnBudgetHistory.setOnClickListener(v -> {
                showBudgetHistoryDialog();
            });
        }

        btnSetTotalBudget.setOnClickListener(v -> {
            BudgetDialogHelper.showSetBudgetDialog(
                    requireContext(),
                    repository,
                    currentMonth,
                    currentTotalBudget,
                    false,
                    () -> observeBudgetData()
            );
        });

        btnAddCategoryBudget.setOnClickListener(v -> {
            BudgetDialogHelper.showSetBudgetDialog(
                    requireContext(),
                    repository,
                    currentMonth,
                    null,
                    true,
                    () -> observeBudgetData()
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

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            observeBudgetData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            observeBudgetData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeBudgetDataObservers();
    }

    private void removeBudgetDataObservers() {
        if (liveMonthlySpent != null) {
            try { liveMonthlySpent.removeObservers(getViewLifecycleOwner()); } catch (Exception ignored) {}
            liveMonthlySpent = null;
        }
        if (liveTotalBudget != null) {
            try { liveTotalBudget.removeObservers(getViewLifecycleOwner()); } catch (Exception ignored) {}
            liveTotalBudget = null;
        }
        if (liveCategoryBudgets != null) {
            try { liveCategoryBudgets.removeObservers(getViewLifecycleOwner()); } catch (Exception ignored) {}
            liveCategoryBudgets = null;
        }
    }

    private void observeBudgetData() {
        if (!isAdded()) return;
        removeBudgetDataObservers();

        currentMonth = DateUtils.formatMonthIso(selectedBudgetCalendar);
        if (tvMonth != null) {
            tvMonth.setText(DateUtils.formatDisplayMonth(selectedBudgetCalendar));
        }

        // Chi tiêu tổng của tháng
        liveMonthlySpent = repository.getMonthlyExpense(currentMonth);
        liveMonthlySpent.observe(getViewLifecycleOwner(), spent -> {
            currentMonthSpent = spent != null ? spent : 0.0;
            updateTotalBudgetUi();
        });

        // Ngân sách tổng của tháng
        liveTotalBudget = repository.getTotalBudgetByMonth(currentMonth);
        liveTotalBudget.observe(getViewLifecycleOwner(), budget -> {
            currentTotalBudget = budget;
            updateTotalBudgetUi();
        });

        // Ngân sách của các danh mục trong tháng
        liveCategoryBudgets = repository.getBudgetsByMonth(currentMonth);
        liveCategoryBudgets.observe(getViewLifecycleOwner(), budgets -> {
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

    private void showSelectMonthDialog() {
        if (!isAdded() || getContext() == null) return;

        Calendar tempCal = (Calendar) selectedBudgetCalendar.clone();
        int currentYear = tempCal.get(Calendar.YEAR);

        String[] monthNames = new String[]{
                "Thg 1", "Thg 2", "Thg 3", "Thg 4",
                "Thg 5", "Thg 6", "Thg 7", "Thg 8",
                "Thg 9", "Thg 10", "Thg 11", "Thg 12"
        };

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvYear = dialogView.findViewById(R.id.tv_picker_year);
        View btnPrevYear = dialogView.findViewById(R.id.btn_picker_prev_year);
        View btnNextYear = dialogView.findViewById(R.id.btn_picker_next_year);
        GridLayout gridMonths = dialogView.findViewById(R.id.grid_months);
        View btnCurrentMonth = dialogView.findViewById(R.id.btn_quick_current_month);
        View btnPrevMonth = dialogView.findViewById(R.id.btn_quick_prev_month);

        int[] selectedYear = {currentYear};
        tvYear.setText(String.valueOf(selectedYear[0]));

        btnPrevYear.setOnClickListener(v -> {
            selectedYear[0]--;
            tvYear.setText(String.valueOf(selectedYear[0]));
        });

        btnNextYear.setOnClickListener(v -> {
            selectedYear[0]++;
            tvYear.setText(String.valueOf(selectedYear[0]));
        });

        if (gridMonths != null) {
            for (int i = 0; i < gridMonths.getChildCount(); i++) {
                final int monthIndex = i;
                View child = gridMonths.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setText(monthNames[i]);
                    child.setOnClickListener(v -> {
                        selectedBudgetCalendar.set(Calendar.YEAR, selectedYear[0]);
                        selectedBudgetCalendar.set(Calendar.MONTH, monthIndex);
                        selectedBudgetCalendar.set(Calendar.DAY_OF_MONTH, 1);
                        dialog.dismiss();
                        observeBudgetData();
                    });
                }
            }
        }

        btnCurrentMonth.setOnClickListener(v -> {
            selectedBudgetCalendar = Calendar.getInstance();
            dialog.dismiss();
            observeBudgetData();
        });

        btnPrevMonth.setOnClickListener(v -> {
            selectedBudgetCalendar = Calendar.getInstance();
            selectedBudgetCalendar.add(Calendar.MONTH, -1);
            dialog.dismiss();
            observeBudgetData();
        });

        dialog.show();
    }

    private void showBudgetHistoryDialog() {
        if (!isAdded() || getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_budget_history, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RecyclerView rvHistory = dialogView.findViewById(R.id.rv_budget_history);
        View layoutEmpty = dialogView.findViewById(R.id.layout_empty_budget_history);
        View btnClose = dialogView.findViewById(R.id.btn_close_budget_history);

        BudgetHistoryAdapter historyAdapter = new BudgetHistoryAdapter(requireContext(), month -> {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault());
                java.util.Date d = sdf.parse(month);
                if (d != null) {
                    selectedBudgetCalendar.setTime(d);
                    dialog.dismiss();
                    observeBudgetData();
                }
            } catch (Exception ignored) {}
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(historyAdapter);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        repository.getBudgetHistory(list -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (list == null || list.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvHistory.setVisibility(View.VISIBLE);
                    historyAdapter.setItems(list);
                }
            });
        });

        dialog.show();
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
                () -> observeBudgetData()
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
                                observeBudgetData();
                            });
                        }
                    });
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
