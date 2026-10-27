package com.example.quanlychitieu.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.SavingsGoalAdapter;
import com.example.quanlychitieu.model.SavingsGoal;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.ui.dialog.SavingsDialogHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavingsFragment extends Fragment implements SavingsGoalAdapter.OnSavingsGoalActionListener {

    private ExpenseRepository repository;
    private SavingsGoalAdapter adapter;

    private RecyclerView rvSavings;
    private LinearLayout layoutEmpty;
    private ExtendedFloatingActionButton fabAdd;
    private EditText etSearchSavings;
    private Chip chipSortSavings;

    private List<SavingsGoal> allGoals = new ArrayList<>();
    private String searchKeyword = "";
    private int sortMode = 0; // 0: Tiến độ ↓, 1: Tiền mục tiêu ↓, 2: Tên A-Z

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_savings, container, false);

        repository = new ExpenseRepository(requireActivity().getApplication());

        rvSavings = view.findViewById(R.id.rv_savings_goals);
        layoutEmpty = view.findViewById(R.id.layout_empty_savings);
        fabAdd = view.findViewById(R.id.fab_add_savings);
        etSearchSavings = view.findViewById(R.id.et_search_savings);
        chipSortSavings = view.findViewById(R.id.chip_sort_savings);

        setupRecyclerView();
        setupListeners();
        observeGoals();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new SavingsGoalAdapter(requireContext(), this);
        rvSavings.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSavings.setAdapter(adapter);
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            SavingsDialogHelper.showAddEditDialog(requireContext(), repository, null, null);
        });

        if (etSearchSavings != null) {
            etSearchSavings.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchKeyword = s != null ? s.toString().trim() : "";
                    filterAndSortSavingsGoals();
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (chipSortSavings != null) {
            chipSortSavings.setOnClickListener(v -> {
                sortMode = (sortMode + 1) % 3;
                if (sortMode == 0) {
                    chipSortSavings.setText("Tiến độ ↓");
                } else if (sortMode == 1) {
                    chipSortSavings.setText("Mục tiêu ↓");
                } else {
                    chipSortSavings.setText("Tên A-Z");
                }
                filterAndSortSavingsGoals();
            });
        }
    }

    private void observeGoals() {
        repository.getAllSavingsGoals().observe(getViewLifecycleOwner(), list -> {
            allGoals = list != null ? list : new ArrayList<>();
            filterAndSortSavingsGoals();
        });
    }

    private void filterAndSortSavingsGoals() {
        List<SavingsGoal> filtered = new ArrayList<>();
        for (SavingsGoal goal : allGoals) {
            String name = goal.getName();
            if (searchKeyword.isEmpty() || (name != null && name.toLowerCase().contains(searchKeyword.toLowerCase()))) {
                filtered.add(goal);
            }
        }

        Collections.sort(filtered, (o1, o2) -> {
            if (sortMode == 0) {
                double p1 = o1.getTargetAmount() > 0 ? (o1.getCurrentAmount() / o1.getTargetAmount()) : 0;
                double p2 = o2.getTargetAmount() > 0 ? (o2.getCurrentAmount() / o2.getTargetAmount()) : 0;
                return Double.compare(p2, p1);
            } else if (sortMode == 1) {
                return Double.compare(o2.getTargetAmount(), o1.getTargetAmount());
            } else {
                String n1 = o1.getName() != null ? o1.getName() : "";
                String n2 = o2.getName() != null ? o2.getName() : "";
                return n1.compareToIgnoreCase(n2);
            }
        });

        if (filtered.isEmpty()) {
            rvSavings.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvSavings.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            adapter.setGoals(filtered);
        }
    }

    @Override
    public void onDeposit(SavingsGoal goal) {
        SavingsDialogHelper.showDepositDialog(requireContext(), repository, goal, null);
    }

    @Override
    public void onEdit(SavingsGoal goal) {
        SavingsDialogHelper.showAddEditDialog(requireContext(), repository, goal, null);
    }

    @Override
    public void onDelete(SavingsGoal goal) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage("Bạn có chắc chắn muốn xóa mục tiêu tiết kiệm \"" + goal.getName() + "\" không?")
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    repository.deleteSavingsGoal(goal, res -> {
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
