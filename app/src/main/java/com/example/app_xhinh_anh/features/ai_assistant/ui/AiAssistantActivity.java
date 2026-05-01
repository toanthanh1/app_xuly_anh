package com.example.app_xhinh_anh.features.ai_assistant.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_xhinh_anh.R;
import com.example.app_xhinh_anh.BuildConfig;
import com.example.app_xhinh_anh.features.ai_assistant.data.GeminiApiClient;
import com.example.app_xhinh_anh.features.ai_assistant.domain.AiResponseManager;
import com.example.app_xhinh_anh.features.ai_assistant.domain.model.ChatMessage;
import com.example.app_xhinh_anh.features.ai_assistant.ui.adapter.ChatAdapter;

public class AiAssistantActivity extends AppCompatActivity {

    private RecyclerView rvChatHistory;
    private ChatAdapter chatAdapter;
    private EditText etChatInput;
    private ImageButton btnSendChat;
    private ProgressBar pbAiThinking;
    private GeminiApiClient geminiApiClient;

    // Callback xử lý phản hồi tập trung
    private final AiResponseManager.ResponseCallback aiCallback = new AiResponseManager.ResponseCallback() {
        @Override
        public void onMessage(String text) {
            runOnUiThread(() -> {
                chatAdapter.addMessage(new ChatMessage(text, false));
                rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            });
        }

        @Override
        public void onApplyFilter(String filterName) {
            // Gửi lệnh về EditorActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("action", "APPLY_FILTER");
            resultIntent.putExtra("filter_name", filterName);
            setResult(RESULT_OK, resultIntent);
            // Có thể thêm thông báo hoặc đóng chat tùy UX
        }

        @Override
        public void onAdjust(String property, int value) {
            // Logic điều chỉnh thông số (Brightness, Contrast...)
        }

        @Override
        public void onError(String error) {
            onMessage("❌ Lỗi: " + error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        setupToolbar();
        initViews();
        setupChat();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvChatHistory = findViewById(R.id.rvChatHistory);
        etChatInput = findViewById(R.id.etChatInput);
        btnSendChat = findViewById(R.id.btnSendChat);
        pbAiThinking = findViewById(R.id.pbAiThinking);
    }

    private void setupChat() {
        chatAdapter = new ChatAdapter();
        rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        rvChatHistory.setAdapter(chatAdapter);

        geminiApiClient = new GeminiApiClient(BuildConfig.GEMINI_API_KEY);

        // ĐẦU VÀO: Hướng dẫn ngắn gọn cho người dùng khi bắt đầu
        if (chatAdapter.getItemCount() == 0) {
            String welcomeMsg = "👋 Chào bạn! Tôi là Trợ lý AI.\n\n" +
                    "Bạn có thể yêu cầu tôi:\n" +
                    "✨ 'Làm trắng da' hoặc 'Ảnh sáng hơn'\n" +
                    "🎨 'Áp dụng bộ lọc Hoài cổ'\n" +
                    "💬 Hoặc chỉ đơn giản là hỏi cách chỉnh ảnh đẹp.\n\n" +
                    "Tôi có thể giúp gì cho bạn ngay bây giờ?";
            chatAdapter.addMessage(new ChatMessage(welcomeMsg, false));
        }

        btnSendChat.setOnClickListener(v -> {
            String message = etChatInput.getText().toString().trim();
            if (!message.isEmpty()) {
                processUserMessage(message);
            }
        });
    }

    private void processUserMessage(String message) {
        // Hiển thị tin nhắn của người dùng
        chatAdapter.addMessage(new ChatMessage(message, true));
        etChatInput.setText("");
        rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        // TRUNG GIAN (ĐẦU VÀO): Kiểm tra xử lý cục bộ để giảm tải server
        if (AiResponseManager.handleLocalInput(message, aiCallback)) {
            return;
        }

        // Gửi lên AI nếu không xử lý cục bộ được
        pbAiThinking.setVisibility(View.VISIBLE);
        geminiApiClient.sendMessage(message, new GeminiApiClient.AiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    pbAiThinking.setVisibility(View.GONE);
                    // TRUNG GIAN (ĐẦU RA): Parse kết quả để hiển thị thân thiện
                    AiResponseManager.parseResponse(response, aiCallback);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    pbAiThinking.setVisibility(View.GONE);
                    aiCallback.onError(t.getMessage());
                });
            }
        });
    }
}
