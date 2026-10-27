package com.example.quanlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.PendingTransaction;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PendingTransactionAdapter extends RecyclerView.Adapter<PendingTransactionAdapter.ViewHolder> {

    public interface OnPendingActionListener {
        void onApprove(PendingTransaction item);
        void onDismiss(PendingTransaction item);
    }

    private final Context context;
    private List<PendingTransaction> list = new ArrayList<>();
    private final OnPendingActionListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());

    public PendingTransactionAdapter(Context context, OnPendingActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTransactions(List<PendingTransaction> items) {
        this.list = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_pending_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingTransaction item = list.get(position);

        holder.tvBank.setText(item.getBankName());

        boolean isIncome = "INCOME".equalsIgnoreCase(item.getType());
        if (isIncome) {
            holder.tvType.setText("Thu nhập");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_income);
            holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.income_green));

            holder.tvAmount.setText(CurrencyUtils.formatVndWithSign(item.getAmount(), "INCOME"));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green));
        } else {
            holder.tvType.setText("Chi tiêu");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_expense);
            holder.tvType.setTextColor(ContextCompat.getColor(context, R.color.expense_red));

            holder.tvAmount.setText(CurrencyUtils.formatVndWithSign(item.getAmount(), "EXPENSE"));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
        }

        String raw = item.getRawContent() != null && !item.getRawContent().trim().isEmpty()
                ? item.getRawContent()
                : item.getNote();
        holder.tvRaw.setText(raw != null ? raw : "Không có chi tiết");

        String timeStr = item.getTimestamp() > 0
                ? timeFormat.format(new Date(item.getTimestamp()))
                : DateUtils.formatDisplayDate(item.getDate());
        holder.tvTime.setText(timeStr);

        String cat = item.getSuggestedCategory() != null && !item.getSuggestedCategory().isEmpty()
                ? item.getSuggestedCategory()
                : (isIncome ? "Thu nhập khác" : "Chi tiêu khác");
        holder.tvCategory.setText(cat);

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(item);
        });

        holder.btnDismiss.setOnClickListener(v -> {
            if (listener != null) listener.onDismiss(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBank, tvType, tvAmount, tvTime, tvRaw, tvCategory;
        Button btnDismiss, btnApprove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBank = itemView.findViewById(R.id.tv_pending_item_bank);
            tvType = itemView.findViewById(R.id.tv_pending_item_type);
            tvAmount = itemView.findViewById(R.id.tv_pending_item_amount);
            tvTime = itemView.findViewById(R.id.tv_pending_item_time);
            tvRaw = itemView.findViewById(R.id.tv_pending_item_raw);
            tvCategory = itemView.findViewById(R.id.tv_pending_item_category);
            btnDismiss = itemView.findViewById(R.id.btn_item_dismiss);
            btnApprove = itemView.findViewById(R.id.btn_item_approve);
        }
    }
}
