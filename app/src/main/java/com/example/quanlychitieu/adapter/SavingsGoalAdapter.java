package com.example.quanlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.SavingsGoal;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SavingsGoalAdapter extends RecyclerView.Adapter<SavingsGoalAdapter.SavingsViewHolder> {

    public interface OnSavingsGoalActionListener {
        void onDeposit(SavingsGoal goal);
        void onEdit(SavingsGoal goal);
        void onDelete(SavingsGoal goal);
    }

    private final Context context;
    private List<SavingsGoal> goals = new ArrayList<>();
    private final OnSavingsGoalActionListener listener;

    public SavingsGoalAdapter(Context context, OnSavingsGoalActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setGoals(List<SavingsGoal> goals) {
        this.goals = goals != null ? goals : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SavingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_savings_goal, parent, false);
        return new SavingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavingsViewHolder holder, int position) {
        SavingsGoal goal = goals.get(position);

        holder.tvName.setText(goal.getName());
        holder.tvCurrent.setText(CurrencyUtils.formatVnd(goal.getCurrentAmount()));
        holder.tvTarget.setText(CurrencyUtils.formatVnd(goal.getTargetAmount()));

        int percent = goal.getTargetAmount() > 0 
                ? (int) Math.round((goal.getCurrentAmount() / goal.getTargetAmount()) * 100) 
                : 0;

        holder.tvPercent.setText(percent + "%");
        holder.progressBar.setProgress(Math.min(percent, 100));

        boolean completed = goal.getCurrentAmount() >= goal.getTargetAmount();
        if (completed) {
            holder.tvCompletedBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvCompletedBadge.setVisibility(View.GONE);
        }

        holder.btnDeposit.setOnClickListener(v -> {
            if (listener != null) listener.onDeposit(goal);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(goal);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(goal);
        });
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    static class SavingsViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCurrent, tvTarget, tvPercent, tvCompletedBadge;
        ProgressBar progressBar;
        ImageView btnEdit, btnDelete;
        MaterialButton btnDeposit;

        public SavingsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_savings_name);
            tvCurrent = itemView.findViewById(R.id.tv_savings_current);
            tvTarget = itemView.findViewById(R.id.tv_savings_target);
            tvPercent = itemView.findViewById(R.id.tv_savings_percent);
            tvCompletedBadge = itemView.findViewById(R.id.tv_savings_completed_badge);
            progressBar = itemView.findViewById(R.id.progress_savings);
            btnEdit = itemView.findViewById(R.id.btn_edit_savings);
            btnDelete = itemView.findViewById(R.id.btn_delete_savings);
            btnDeposit = itemView.findViewById(R.id.btn_add_savings_money);
        }
    }
}
