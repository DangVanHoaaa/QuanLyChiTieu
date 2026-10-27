package com.example.quanlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.Budget;
import com.example.quanlychitieu.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    public static class BudgetItemModel {
        private final Budget budget;
        private final double spentAmount;

        public BudgetItemModel(Budget budget, double spentAmount) {
            this.budget = budget;
            this.spentAmount = spentAmount;
        }

        public Budget getBudget() {
            return budget;
        }

        public double getSpentAmount() {
            return spentAmount;
        }
    }

    public interface OnBudgetActionListener {
        void onEdit(Budget budget);
        void onDelete(Budget budget);
    }

    private final Context context;
    private List<BudgetItemModel> items = new ArrayList<>();
    private final OnBudgetActionListener listener;

    public BudgetAdapter(Context context, OnBudgetActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<BudgetItemModel> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetItemModel model = items.get(position);
        Budget budget = model.getBudget();
        double spent = model.getSpentAmount();
        double total = budget.getAmount();
        double remaining = total - spent;
        int percent = total > 0 ? (int) Math.round((spent / total) * 100) : 0;

        holder.tvName.setText(budget.getCategoryName());
        holder.tvAmount.setText(CurrencyUtils.formatVnd(total));
        holder.tvSpent.setText("Đã chi: " + CurrencyUtils.formatVnd(spent));

        if (remaining >= 0) {
            holder.tvRemaining.setText("Còn lại: " + CurrencyUtils.formatVnd(remaining));
            holder.tvRemaining.setTextColor(ContextCompat.getColor(context, R.color.primary_dark));
        } else {
            holder.tvRemaining.setText("Vượt mức: " + CurrencyUtils.formatVnd(Math.abs(remaining)));
            holder.tvRemaining.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
        }

        holder.progressBar.setProgress(Math.min(percent, 100));

        // Cảnh báo khi đã dùng >= 80% hoặc vượt mức
        if (spent > total) {
            holder.tvAlert.setVisibility(View.VISIBLE);
            holder.tvAlert.setText("Cảnh báo: Bạn đã chi tiêu vượt quá ngân sách danh mục này!");
            holder.tvAlert.setBackgroundResource(R.drawable.bg_badge_expense);
            holder.tvAlert.setTextColor(ContextCompat.getColor(context, R.color.danger_red));
        } else if (percent >= 80) {
            holder.tvAlert.setVisibility(View.VISIBLE);
            holder.tvAlert.setText("Chú ý: Đã chi tiêu hơn " + percent + "% ngân sách!");
            holder.tvAlert.setBackgroundResource(R.drawable.bg_badge_warning);
            holder.tvAlert.setTextColor(ContextCompat.getColor(context, R.color.warning_orange));
        } else {
            holder.tvAlert.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(budget);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(budget);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAmount, tvSpent, tvRemaining, tvAlert;
        ProgressBar progressBar;
        ImageView btnEdit, btnDelete;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_budget_cat_name);
            tvAmount = itemView.findViewById(R.id.tv_budget_cat_amount);
            tvSpent = itemView.findViewById(R.id.tv_budget_cat_spent);
            tvRemaining = itemView.findViewById(R.id.tv_budget_cat_remaining);
            tvAlert = itemView.findViewById(R.id.tv_budget_cat_alert);
            progressBar = itemView.findViewById(R.id.progress_cat_budget);
            btnEdit = itemView.findViewById(R.id.btn_edit_budget);
            btnDelete = itemView.findViewById(R.id.btn_delete_budget);
        }
    }
}
