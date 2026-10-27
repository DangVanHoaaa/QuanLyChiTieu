package com.example.quanlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.model.ChatMessage;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.utils.CurrencyUtils;
import com.example.quanlychitieu.utils.DateUtils;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnTransactionConfirmListener {
        void onConfirm(ChatMessage message, Transaction transaction);
        void onCancel(ChatMessage message);
    }

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final Context context;
    private List<ChatMessage> messages = new ArrayList<>();
    private final OnTransactionConfirmListener confirmListener;

    public ChatAdapter(Context context, OnTransactionConfirmListener confirmListener) {
        this.context = context;
        this.confirmListener = confirmListener;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            messages.add(message);
            notifyItemInserted(messages.size() - 1);
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_bot, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        String timeStr = TIME_FORMAT.format(new Date(message.getTimestamp()));

        if (holder instanceof UserMessageViewHolder) {
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;
            userHolder.tvMessage.setText(message.getContent());
            userHolder.tvTime.setText(timeStr);
        } else if (holder instanceof BotMessageViewHolder) {
            BotMessageViewHolder botHolder = (BotMessageViewHolder) holder;
            botHolder.tvMessage.setText(message.getContent());
            botHolder.tvTime.setText(timeStr);

            // Xử lý thẻ xác nhận giao dịch
            if (message.isPendingConfirmation() && message.getProposedTransaction() != null) {
                botHolder.cardConfirmation.setVisibility(View.VISIBLE);
                Transaction t = message.getProposedTransaction();

                String summary = ("INCOME".equals(t.getType()) ? "Thu nhập: " : "Chi tiêu: ")
                        + t.getCategoryName() + " • " + CurrencyUtils.formatVnd(t.getAmount());
                botHolder.tvConfirmSummary.setText(summary);

                if (message.isConfirmed()) {
                    botHolder.layoutButtons.setVisibility(View.GONE);
                    botHolder.tvStatus.setVisibility(View.VISIBLE);
                    botHolder.tvStatus.setText("✅ Đã lưu thành công vào sổ chi tiêu!");
                    botHolder.tvStatus.setTextColor(context.getColor(R.color.income_green));
                } else if (message.isCancelled()) {
                    botHolder.layoutButtons.setVisibility(View.GONE);
                    botHolder.tvStatus.setVisibility(View.VISIBLE);
                    botHolder.tvStatus.setText("❌ Bạn đã hủy giao dịch này.");
                    botHolder.tvStatus.setTextColor(context.getColor(R.color.text_secondary));
                } else {
                    botHolder.layoutButtons.setVisibility(View.VISIBLE);
                    botHolder.tvStatus.setVisibility(View.GONE);

                    botHolder.btnConfirm.setOnClickListener(v -> {
                        message.setConfirmed(true);
                        notifyItemChanged(position);
                        if (confirmListener != null) {
                            confirmListener.onConfirm(message, t);
                        }
                    });

                    botHolder.btnCancel.setOnClickListener(v -> {
                        message.setCancelled(true);
                        notifyItemChanged(position);
                        if (confirmListener != null) {
                            confirmListener.onCancel(message);
                        }
                    });
                }
            } else {
                botHolder.cardConfirmation.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_user_message);
            tvTime = itemView.findViewById(R.id.tv_user_time);
        }
    }

    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        CardView cardConfirmation;
        TextView tvConfirmSummary, tvStatus;
        LinearLayout layoutButtons;
        MaterialButton btnConfirm, btnCancel;

        public BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_bot_message);
            tvTime = itemView.findViewById(R.id.tv_bot_time);
            cardConfirmation = itemView.findViewById(R.id.card_confirmation);
            tvConfirmSummary = itemView.findViewById(R.id.tv_confirm_summary);
            tvStatus = itemView.findViewById(R.id.tv_confirmation_status);
            layoutButtons = itemView.findViewById(R.id.layout_action_buttons);
            btnConfirm = itemView.findViewById(R.id.btn_confirm_transaction);
            btnCancel = itemView.findViewById(R.id.btn_cancel_transaction);
        }
    }
}
