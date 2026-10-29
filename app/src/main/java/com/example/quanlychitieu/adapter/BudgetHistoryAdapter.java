package com.example.quanlychitieu.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class BudgetHistoryAdapter extends RecyclerView.Adapter<BudgetHistoryAdapter.ViewHolder> {

    public interface OnHistoryMonthClickListener {
        void onMonthSelected(String month);
    }

    private final Context context;
    private final List<ExpenseRepository.BudgetHistoryItem> items = new ArrayList<>();
    private final OnHistoryMonthClickListener listener;

    public BudgetHistoryAdapter(Context context, OnHistoryMonthClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<ExpenseRepository.BudgetHistoryItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpenseRepository.BudgetHistoryItem item = items.get(position);

        holder.tvMonth.setText(DateUtils.formatDisplayMonth(item.getMonth()));
        holder.tvSpent.setText(CurrencyUtils.formatVnd(item.getTotalSpent()));

        if (!item.isHasBudget() || item.getTotalBudget() <= 0) {
            holder.tvBudget.setText("Chưa đặt");
            holder.progress.setProgress(0);
            holder.tvPercent.setText("Chưa đặt ngân sách");
            holder.tvDiff.setText("Đã chi: " + CurrencyUtils.formatVnd(item.getTotalSpent()));
            holder.tvDiff.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));

            holder.tvBadge.setText("Chưa đặt NS");
            holder.tvBadge.setBackgroundResource(R.drawable.bg_rounded_card);
            holder.tvBadge.setTextColor(Color.parseColor("#64748B"));
        } else {
            holder.tvBudget.setText(CurrencyUtils.formatVnd(item.getTotalBudget()));
            int percent = item.getPercent();
            holder.progress.setProgress(Math.min(percent, 100));
            holder.tvPercent.setText("Đã dùng: " + percent + "%");

            double remaining = item.getRemaining();
            if (remaining >= 0) {
                holder.tvBadge.setText("✅ Đạt ngân sách");
                holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_income);
                holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.income_green));

                holder.tvDiff.setText("Tiết kiệm: " + CurrencyUtils.formatVnd(remaining));
                holder.tvDiff.setTextColor(ContextCompat.getColor(context, R.color.income_green));
            } else {
                holder.tvBadge.setText("⚠️ Vượt ngân sách");
                holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_expense);
                holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.expense_red));

                holder.tvDiff.setText("Vượt mức: " + CurrencyUtils.formatVnd(Math.abs(remaining)));
                holder.tvDiff.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMonthSelected(item.getMonth());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvBadge, tvBudget, tvSpent, tvPercent, tvDiff;
        ProgressBar progress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tv_history_month);
            tvBadge = itemView.findViewById(R.id.tv_history_badge);
            tvBudget = itemView.findViewById(R.id.tv_history_budget_amount);
            tvSpent = itemView.findViewById(R.id.tv_history_spent_amount);
            tvPercent = itemView.findViewById(R.id.tv_history_percent);
            tvDiff = itemView.findViewById(R.id.tv_history_diff_text);
            progress = itemView.findViewById(R.id.progress_history_budget);
        }
    }
}
