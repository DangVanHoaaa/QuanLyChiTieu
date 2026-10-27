package com.example.quanlychitieu.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.quanlychitieu.adapter.TransactionAdapter;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.ui.dialog.TransactionDialogHelper;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionsFragment extends Fragment implements TransactionAdapter.OnTransactionActionListener {

    private ExpenseRepository repository;
    private TransactionAdapter adapter;

    private EditText etSearch;
    private Chip chipAll, chipIncome, chipExpense, chipSort;
    private RecyclerView rvTransactions;
    private LinearLayout layoutEmpty;
    private FloatingActionButton fabAdd;

    private List<Transaction> allTransactions = new ArrayList<>();
    private String currentTypeFilter = "ALL"; // ALL, INCOME, EXPENSE
    private boolean isSortNewest = true;
    private String currentKeyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        repository = new ExpenseRepository(requireActivity().getApplication());

        etSearch = view.findViewById(R.id.et_search_note);
        chipAll = view.findViewById(R.id.chip_all);
        chipIncome = view.findViewById(R.id.chip_income);
        chipExpense = view.findViewById(R.id.chip_expense);
        chipSort = view.findViewById(R.id.chip_sort);
        rvTransactions = view.findViewById(R.id.rv_transactions);
        layoutEmpty = view.findViewById(R.id.layout_empty_transactions);
        fabAdd = view.findViewById(R.id.fab_add);
        View btnOpenAi = view.findViewById(R.id.btn_open_ai_chatbot_trans);
        if (btnOpenAi != null) {
            btnOpenAi.setOnClickListener(v -> {
                startActivity(new android.content.Intent(requireContext(), ChatBotActivity.class));
            });
        }

        setupRecyclerView();
        setupFiltersAndSearch();
        setupListeners();
        observeTransactions();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(requireContext(), this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void setupFiltersAndSearch() {
        chipAll.setOnClickListener(v -> {
            currentTypeFilter = "ALL";
            applyFilterAndSort();
        });

        chipIncome.setOnClickListener(v -> {
            currentTypeFilter = "INCOME";
            applyFilterAndSort();
        });

        chipExpense.setOnClickListener(v -> {
            currentTypeFilter = "EXPENSE";
            applyFilterAndSort();
        });

        chipSort.setOnClickListener(v -> {
            isSortNewest = !isSortNewest;
            chipSort.setText(isSortNewest ? "Mới nhất ↓" : "Cũ nhất ↑");
            applyFilterAndSort();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentKeyword = s != null ? s.toString().trim() : "";
                applyFilterAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            TransactionDialogHelper.showAddEditDialog(requireContext(), repository, null, null);
        });
    }

    private void observeTransactions() {
        repository.getAllTransactions().observe(getViewLifecycleOwner(), list -> {
            allTransactions = list != null ? new ArrayList<>(list) : new ArrayList<>();
            applyFilterAndSort();
        });
    }

    private void applyFilterAndSort() {
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction item : allTransactions) {
            // Lọc theo loại
            if (!"ALL".equals(currentTypeFilter) && !currentTypeFilter.equalsIgnoreCase(item.getType())) {
                continue;
            }
            // Lọc theo từ khóa tìm kiếm trong ghi chú hoặc tên danh mục
            if (!currentKeyword.isEmpty()) {
                boolean matchNote = item.getNote() != null && item.getNote().toLowerCase().contains(currentKeyword.toLowerCase());
                boolean matchCat = item.getCategoryName() != null && item.getCategoryName().toLowerCase().contains(currentKeyword.toLowerCase());
                if (!matchNote && !matchCat) {
                    continue;
                }
            }
            filtered.add(item);
        }

        // Sắp xếp
        Collections.sort(filtered, (t1, t2) -> {
            int cmp = t1.getDate().compareTo(t2.getDate());
            if (cmp == 0) {
                cmp = Integer.compare(t1.getId(), t2.getId());
            }
            return isSortNewest ? -cmp : cmp;
        });

        adapter.setTransactions(filtered);

        if (filtered.isEmpty()) {
            rvTransactions.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvTransactions.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEdit(Transaction transaction) {
        TransactionDialogHelper.showAddEditDialog(requireContext(), repository, transaction, null);
    }

    @Override
    public void onDelete(Transaction transaction) {
        boolean isIncome = "INCOME".equalsIgnoreCase(transaction.getType());
        String amountFormatted = CurrencyUtils.formatVnd(transaction.getAmount());
        String message;
        if (isIncome) {
            message = "Bạn đang chọn xóa khoản Thu nhập: + " + amountFormatted + " (" + transaction.getCategoryName() + ")\n\n"
                    + "⚠️ Lưu ý: Vì đây là khoản thu vào ví, khi xóa thì Tổng số dư hiện tại sẽ bị TRỪ ĐI " + amountFormatted + ".\n\n"
                    + "Bạn có chắc chắn muốn xóa không?";
        } else {
            message = "Bạn đang chọn xóa khoản Chi tiêu: - " + amountFormatted + " (" + transaction.getCategoryName() + ")\n\n"
                    + "💡 Khoản chi này sẽ bị hủy bỏ, tiền sẽ được hoàn lại giúp Tổng số dư hiện tại TĂNG THÊM " + amountFormatted + ".\n\n"
                    + "Bạn có chắc chắn muốn xóa không?";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(message)
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
