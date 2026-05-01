package com.example.app_xhinh_anh.features.ai_assistant.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
            Intent resultIntent = new Intent();
            resultIntent.putExtra("action", "APPLY_FILTER");
            resultIntent.putExtra("filter_name", filterName);
            setResult(RESULT_OK, resultIntent);
            
            // TỰ ĐỘNG THOÁT: Để người dùng thấy kết quả trên ảnh ngay lập tức
            new android.os.Handler().postDelayed(() -> finish(), 1500);
        }

        @Override
        public void onAdjust(String property, int value) {
            // Xử lý điều chỉnh thông số nếu cần
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

        if (chatAdapter.getItemCount() == 0) {
            chatAdapter.addMessage(new ChatMessage("👋 Chào bạn! Tôi có thể giúp:\n✨ 'Làm trắng da'\n🎨 'Bộ lọc Hoài cổ'\nHoặc đặt câu hỏi bất kỳ.", false));
        }

        btnSendChat.setOnClickListener(v -> {
            String message = etChatInput.getText().toString().trim();
            if (!message.isEmpty()) {
                hideKeyboard();
                processMessage(message);
            }
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void processMessage(String message) {
        chatAdapter.addMessage(new ChatMessage(message, true));
        etChatInput.setText("");
        rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        // Kiểm tra lệnh cục bộ để phản hồi tức thì
        if (AiResponseManager.handleLocalInput(message, aiCallback)) {
            return;
        }

        pbAiThinking.setVisibility(View.VISIBLE);
        geminiApiClient.sendMessage(message, new GeminiApiClient.AiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    pbAiThinking.setVisibility(View.GONE);
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
