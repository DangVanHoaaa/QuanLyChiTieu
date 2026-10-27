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
import com.example.quanlychitieu.utils.CategoryUtils;
import com.example.quanlychitieu.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryStatAdapter extends RecyclerView.Adapter<CategoryStatAdapter.ViewHolder> {

    public static class CategoryStatItem {
        private final String categoryName;
        private final double amount;
        private final float percentage;

        public CategoryStatItem(String categoryName, double amount, float percentage) {
            this.categoryName = categoryName;
            this.amount = amount;
            this.percentage = percentage;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public double getAmount() {
            return amount;
        }

        public float getPercentage() {
            return percentage;
        }
    }

    private final Context context;
    private List<CategoryStatItem> items = new ArrayList<>();

    public CategoryStatAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<CategoryStatItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryStatItem item = items.get(position);
        holder.tvName.setText(item.getCategoryName());
        holder.tvAmount.setText(CurrencyUtils.formatVnd(item.getAmount()));
        holder.tvPercent.setText(String.format(Locale.getDefault(), "(%.1f%%)", item.getPercentage()));
        holder.progress.setProgress(Math.round(item.getPercentage()));

        int iconRes = CategoryUtils.getCategoryIconRes(context, item.getCategoryName());
        holder.ivIcon.setImageResource(iconRes);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvAmount, tvPercent;
        ProgressBar progress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_cat_stat_icon);
            tvName = itemView.findViewById(R.id.tv_cat_stat_name);
            tvAmount = itemView.findViewById(R.id.tv_cat_stat_amount);
            tvPercent = itemView.findViewById(R.id.tv_cat_stat_percent);
            progress = itemView.findViewById(R.id.progress_cat_stat);
        }
    }
}
