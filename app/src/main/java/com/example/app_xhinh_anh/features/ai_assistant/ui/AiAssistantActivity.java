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
import com.example.app_xhinh_anh.features.ai_assistant.domain.model.ChatMessage;
import com.example.app_xhinh_anh.features.ai_assistant.ui.adapter.ChatAdapter;

public class AiAssistantActivity extends AppCompatActivity {

    private RecyclerView rvChatHistory;
    private ChatAdapter chatAdapter;
    private EditText etChatInput;
    private ImageButton btnSendChat;
    private ProgressBar pbAiThinking;
    private GeminiApiClient geminiApiClient;

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

        // API Key được lấy từ BuildConfig (local.properties)
        geminiApiClient = new GeminiApiClient(BuildConfig.GEMINI_API_KEY);

        if (chatAdapter.getItemCount() == 0) {
            chatAdapter.addMessage(new ChatMessage("Xin chào! Tôi là trợ lý AI. Tôi có thể giúp bạn chỉnh sửa ảnh. Hãy thử nói 'Làm ảnh tươi sáng hơn' hoặc 'Áp dụng bộ lọc Retro'.", false));
        }

        btnSendChat.setOnClickListener(v -> {
            String message = etChatInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendChatMessage(message);
            }
        });
    }

    private void sendChatMessage(String message) {
        chatAdapter.addMessage(new ChatMessage(message, true));
        etChatInput.setText("");
        rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        pbAiThinking.setVisibility(View.VISIBLE);
        geminiApiClient.sendMessage(message, new GeminiApiClient.AiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    pbAiThinking.setVisibility(View.GONE);
                    chatAdapter.addMessage(new ChatMessage(response, false));
                    rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    
                    // Gửi kết quả về EditorActivity nếu có action
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("ai_response", response);
                    setResult(RESULT_OK, resultIntent);
                    // Ở đây có thể quyết định finish() hoặc không tùy UX mong muốn
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    pbAiThinking.setVisibility(View.GONE);
                    chatAdapter.addMessage(new ChatMessage("Lỗi: " + t.getMessage(), false));
                    rvChatHistory.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                });
            }
        });
    }
}
