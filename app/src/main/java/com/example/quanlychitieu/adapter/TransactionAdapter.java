package com.example.quanlychitieu.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.utils.CategoryUtils;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public interface OnTransactionActionListener {
        void onEdit(Transaction transaction);
        void onDelete(Transaction transaction);
    }

    private final Context context;
    private List<Transaction> transactions = new ArrayList<>();
    private final OnTransactionActionListener listener;

    public TransactionAdapter(Context context, OnTransactionActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction item = transactions.get(position);

        holder.tvCategoryName.setText(item.getCategoryName());

        if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText(item.getNote());
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        holder.tvDate.setText(DateUtils.formatDisplayDate(item.getDate()));

        boolean isIncome = "INCOME".equalsIgnoreCase(item.getType());
        if (isIncome) {
            holder.tvAmount.setText(CurrencyUtils.formatVndWithSign(item.getAmount(), "INCOME"));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green));
            holder.layoutIcon.setBackgroundResource(R.drawable.bg_badge_income);
            holder.ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.income_green));
        } else {
            holder.tvAmount.setText(CurrencyUtils.formatVndWithSign(item.getAmount(), "EXPENSE"));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
            holder.layoutIcon.setBackgroundResource(R.drawable.bg_badge_expense);
            holder.ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.expense_red));
        }

        int iconRes = CategoryUtils.getCategoryIconRes(context, item.getCategoryName());
        holder.ivCategoryIcon.setImageResource(iconRes);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        FrameLayout layoutIcon;
        ImageView ivCategoryIcon, btnEdit, btnDelete;
        TextView tvCategoryName, tvNote, tvDate, tvAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutIcon = itemView.findViewById(R.id.layout_icon_container);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvNote = itemView.findViewById(R.id.tv_transaction_note);
            tvDate = itemView.findViewById(R.id.tv_transaction_date);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
            btnEdit = itemView.findViewById(R.id.btn_edit_transaction);
            btnDelete = itemView.findViewById(R.id.btn_delete_transaction);
        }
    }
}
