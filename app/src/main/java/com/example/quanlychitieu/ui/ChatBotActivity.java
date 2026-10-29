package com.example.quanlychitieu.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.ChatAdapter;
import com.example.quanlychitieu.ai.AiAssistantManager;
import com.example.quanlychitieu.model.ChatMessage;
import com.example.quanlychitieu.model.Transaction;
import com.example.quanlychitieu.repository.ExpenseRepository;
import com.google.android.material.chip.Chip;

public class ChatBotActivity extends AppCompatActivity implements ChatAdapter.OnTransactionConfirmListener {

    private ImageView btnBack;
    private TextView tvBotStatus;
    private RecyclerView rvChat;
    private LinearLayout layoutTyping;
    private EditText etInput;
    private FrameLayout btnSend;

    private Chip chipQMonth, chipQTopCat, chipQBudget, chipQCompare, chipQSavings, chipQExample;

    private ChatAdapter chatAdapter;
    private AiAssistantManager aiManager;
    private ExpenseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.quanlychitieu.utils.ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        repository = new ExpenseRepository(getApplication());
        aiManager = new AiAssistantManager(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        sendWelcomeMessage();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvBotStatus = findViewById(R.id.tv_bot_status);
        rvChat = findViewById(R.id.rv_chat_messages);
        layoutTyping = findViewById(R.id.layout_bot_typing);
        etInput = findViewById(R.id.et_message_input);
        btnSend = findViewById(R.id.btn_send);

        View btnSettings = findViewById(R.id.btn_ai_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showAiSettingsDialog());
        }

        chipQMonth = findViewById(R.id.chip_q_month);
        chipQTopCat = findViewById(R.id.chip_q_top_cat);
        chipQBudget = findViewById(R.id.chip_q_budget);
        chipQCompare = findViewById(R.id.chip_q_compare);
        chipQSavings = findViewById(R.id.chip_q_savings);
        chipQExample = findViewById(R.id.chip_q_example);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> handleSendMessage());

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            handleSendMessage();
            return true;
        });

        // Xử lý các câu hỏi gợi ý nhanh
        chipQMonth.setOnClickListener(v -> sendMessageWithPrompt("Chi tiêu tháng này của tôi thế nào?"));
        chipQTopCat.setOnClickListener(v -> sendMessageWithPrompt("Tháng này tôi chi tiêu cho danh mục nào nhiều nhất?"));
        chipQBudget.setOnClickListener(v -> sendMessageWithPrompt("Tình hình ngân sách tháng này ra sao?"));
        chipQCompare.setOnClickListener(v -> sendMessageWithPrompt("So sánh chi tiêu tháng này với tháng trước"));
        chipQSavings.setOnClickListener(v -> sendMessageWithPrompt("Tư vấn cho tôi cách tiết kiệm tiền hiệu quả"));
        chipQExample.setOnClickListener(v -> sendMessageWithPrompt("Hôm nay tôi ăn trưa hết 45 nghìn"));
    }

    private void sendWelcomeMessage() {
        String welcome = "Xin chào! Tôi là Trợ lý Tài chính của Ví Thông Minh 🤖\n\n"
                + "Tôi có thể giúp bạn:\n"
                + "• 📊 Xem tổng kết thu chi và ngân sách tháng này\n"
                + "• 🏆 Tìm danh mục chi tiêu tốn kém nhất\n"
                + "• 📈 So sánh chi tiêu với tháng trước\n"
                + "• 💡 Tư vấn cách tiết kiệm tiền hiệu quả\n"
                + "• ⚡ Nhập nhanh giao dịch bằng lời nói/tin nhắn (Ví dụ: 'Ăn trưa 45k', 'Vừa nhận lương 15 triệu'...)\n\n"
                + "Bạn muốn tìm hiểu thông tin gì hôm nay?";

        chatAdapter.addMessage(new ChatMessage(ChatMessage.SENDER_BOT, welcome));
    }

    private void sendMessageWithPrompt(String prompt) {
        etInput.setText(prompt);
        handleSendMessage();
    }

    private void handleSendMessage() {
        String messageText = etInput.getText() != null ? etInput.getText().toString().trim() : "";
        if (messageText.isEmpty()) {
            return;
        }

        // 1. Thêm tin nhắn của User vào danh sách chat
        ChatMessage userMsg = new ChatMessage(ChatMessage.SENDER_USER, messageText);
        chatAdapter.addMessage(userMsg);
        etInput.setText("");
        scrollToBottom();

        // 2. Hiển thị trạng thái đang xử lý
        layoutTyping.setVisibility(View.VISIBLE);
        scrollToBottom();

        // 3. Gửi cho AI Assistant Manager phân tích
        aiManager.processUserMessage(messageText, new AiAssistantManager.AiResponseCallback() {
            @Override
            public void onResponse(ChatMessage response) {
                runOnUiThread(() -> {
                    layoutTyping.setVisibility(View.GONE);
                    chatAdapter.addMessage(response);
                    scrollToBottom();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    layoutTyping.setVisibility(View.GONE);
                    chatAdapter.addMessage(new ChatMessage(ChatMessage.SENDER_BOT, "Xin lỗi, " + errorMessage));
                    scrollToBottom();
                });
            }
        });
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    // --- XỬ LÝ XÁC NHẬN GIAO DỊCH TỪ TIN NHẮN CHATBOT ---
    @Override
    public void onConfirm(ChatMessage message, Transaction transaction) {
        if (transaction == null) return;

        repository.insertTransaction(transaction, id -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã lưu giao dịch vào sổ chi tiêu thành công!", Toast.LENGTH_SHORT).show();
                String confirmReply = "🎉 Tuyệt vời! Tôi đã lưu thành công khoản "
                        + ("INCOME".equals(transaction.getType()) ? "thu nhập" : "chi tiêu")
                        + " **" + transaction.getCategoryName() + "** với số tiền **"
                        + com.example.quanlychitieu.utils.CurrencyUtils.formatVnd(transaction.getAmount())
                        + "** vào sổ chi tiêu và cập nhật số dư cho bạn.";

                chatAdapter.addMessage(new ChatMessage(ChatMessage.SENDER_BOT, confirmReply));
                scrollToBottom();
            });
        });
    }

    @Override
    public void onCancel(ChatMessage message) {
        String cancelReply = "Tôi đã hủy giao dịch theo yêu cầu của bạn. Khoản tiền này chưa được ghi nhận vào hệ thống.";
        chatAdapter.addMessage(new ChatMessage(ChatMessage.SENDER_BOT, cancelReply));
        scrollToBottom();
    }

    private void showAiSettingsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_ai_settings, null);
        com.google.android.material.textfield.TextInputEditText etKey = view.findViewById(R.id.et_gemini_key);
        View btnCancel = view.findViewById(R.id.btn_cancel_ai_settings);
        View btnSave = view.findViewById(R.id.btn_save_ai_settings);

        android.content.SharedPreferences prefs = getSharedPreferences("ai_prefs", MODE_PRIVATE);
        etKey.setText(prefs.getString("gemini_api_key", ""));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String key = etKey.getText() != null ? etKey.getText().toString().trim() : "";
            prefs.edit().putString("gemini_api_key", key).apply();
            dialog.dismiss();
            if (key.isEmpty()) {
                tvBotStatus.setText("AI Offline nội bộ (Sẵn sàng)");
                Toast.makeText(this, "Đã chuyển sang AI Offline nội bộ!", Toast.LENGTH_SHORT).show();
            } else {
                tvBotStatus.setText("Google Gemini AI 1.5 (Đã kết nối)");
                Toast.makeText(this, "Đã lưu Google Gemini API Key!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
