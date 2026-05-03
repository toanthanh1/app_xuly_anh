package com.example.app_xhinh_anh.features.ai_assistant.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_xhinh_anh.BuildConfig;
import com.example.app_xhinh_anh.R;
import com.example.app_xhinh_anh.features.ai_assistant.data.GeminiApiClient;
import com.example.app_xhinh_anh.features.ai_assistant.domain.ActionMapper;
import com.example.app_xhinh_anh.features.ai_assistant.domain.AiResponseManager;
import com.example.app_xhinh_anh.features.ai_assistant.domain.model.ChatMessage;
import com.example.app_xhinh_anh.features.ai_assistant.ui.adapter.ChatAdapter;

import java.util.List;

public class AiAssistantActivity extends AppCompatActivity {

    /** Khoảng chờ trước khi finish() để user còn kịp đọc bubble xác nhận. */
    private static final long FINISH_DELAY_MS = 1000L;

    private RecyclerView rvChatHistory;
    private ChatAdapter chatAdapter;
    private EditText etChatInput;
    private ImageButton btnSendChat;
    private ProgressBar pbAiThinking;
    private GeminiApiClient geminiApiClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final AiResponseManager.ResponseCallback aiCallback = new AiResponseManager.ResponseCallback() {
        @Override
        public void onMessage(String text) {
            runOnUiThread(() -> {
                if (text == null || text.isEmpty()) return;
                chatAdapter.addMessage(new ChatMessage(text, false));
                rvChatHistory.scrollToPosition(chatAdapter.getItemCount() - 1);
            });
        }

        @Override
        public void onApplyFilter(String filterName) {
            Intent r = new Intent();
            r.putExtra("action", "APPLY_FILTER");
            r.putExtra("filter_name", filterName);
            setResult(RESULT_OK, r);
            scheduleFinish();
        }

        @Override
        public void onAdjustments(List<ActionMapper.Adjustment> adjustments) {
            if (adjustments == null || adjustments.isEmpty()) return;
            String[] props = new String[adjustments.size()];
            int[] values = new int[adjustments.size()];
            for (int i = 0; i < adjustments.size(); i++) {
                props[i] = adjustments.get(i).property;
                values[i] = adjustments.get(i).value;
            }
            Intent r = new Intent();
            r.putExtra("action", "ADJUST");
            r.putExtra("adjust_props", props);
            r.putExtra("adjust_values", values);
            setResult(RESULT_OK, r);
            scheduleFinish();
        }

        @Override
        public void onOpenTool(String toolName) {
            Intent r = new Intent();
            r.putExtra("action", "OPEN_TOOL");
            r.putExtra("tool_name", toolName);
            setResult(RESULT_OK, r);
            scheduleFinish();
        }

        @Override
        public void onRemoveBackground() {
            Intent r = new Intent();
            r.putExtra("action", "REMOVE_BACKGROUND");
            setResult(RESULT_OK, r);
            scheduleFinish();
        }

        @Override
        public void onError(String error) {
            onMessage("❌ Lỗi: " + (error != null ? error : "Không xác định"));
            runOnUiThread(() -> setSending(false));
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

    @Override
    protected void onDestroy() {
        // Tránh leak: hủy callback finish nếu activity bị huỷ trước khi delay chạy.
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey != null && !apiKey.isEmpty()) {
            geminiApiClient = new GeminiApiClient(apiKey);
        } else {
            chatAdapter.addMessage(new ChatMessage(
                    "⚠️ Chưa cấu hình GEMINI_API_KEY trong local.properties.\n"
                            + "Chỉ các từ khóa local hoạt động (vd: \"tăng sáng\", \"hoài cổ\", \"xóa nền\").",
                    false));
        }

        if (chatAdapter.getItemCount() == 0
                || (chatAdapter.getItemCount() == 1 && apiKey != null && !apiKey.isEmpty())) {
            chatAdapter.addMessage(new ChatMessage("👋 Chào bạn! Tôi là Trợ lý AI của App Xhinh Anh. Tôi có thể giúp bạn chỉnh sửa ảnh nhanh chóng bằng giọng nói hoặc văn bản.\n\n" +
                    "💡 **Bạn có thể thử các câu lệnh sau:**\n\n" +
                    "🎨 **Áp dụng bộ lọc nhanh:**\n" +
                    "• \"Làm trắng da cho ảnh này\"\n" +
                    "• \"Chỉnh ảnh theo phong cách hoài cổ Polaroid\"\n" +
                    "• \"Dùng bộ lọc Neon Fire cho rực rỡ\"\n\n" +
                    "⚙️ **Điều chỉnh thông số (từ -100 đến 100):**\n" +
                    "• \"Tăng độ sáng ảnh lên 30%\"\n" +
                    "• \"Giảm độ bão hòa màu một chút\"\n" +
                    "• \"Làm ảnh sắc nét hơn (sharpness)\"\n\n" +
                    "✂️ **Công cụ thông minh:**\n" +
                    "• \"Hãy xóa nền cho bức ảnh này giúp tôi\"\n" +
                    "• \"Mở công cụ Curves để tôi tự chỉnh màu\"\n" +
                    "• \"Tôi muốn dùng bảng màu HSL\"\n\n" +
                    "💬 **Tư vấn:** \"Ảnh này hơi tối và mờ, tôi nên làm gì?\"\n\n" +
                    "Bạn muốn thay đổi điều gì cho bức ảnh này?", false));
        }

        btnSendChat.setOnClickListener(v -> {
            if (!btnSendChat.isEnabled()) return;
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
        rvChatHistory.scrollToPosition(chatAdapter.getItemCount() - 1);

        // Bắt từ khóa local trước — phản hồi tức thì, không cần API.
        if (AiResponseManager.handleLocalInput(message, aiCallback)) {
            return;
        }

        if (geminiApiClient == null) {
            aiCallback.onMessage("⚠️ Câu lệnh này cần API key. Hãy cấu hình GEMINI_API_KEY hoặc thử các từ khóa local (vd: \"tăng sáng\", \"hoài cổ\", \"xóa nền\").");
            return;
        }

        setSending(true);
        geminiApiClient.sendMessage(message, new GeminiApiClient.AiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    setSending(false);
                    AiResponseManager.parseResponse(response, aiCallback);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    setSending(false);
                    aiCallback.onError(t != null ? t.getMessage() : null);
                });
            }
        });
    }

    /** Khoá UI gửi tin trong khi chờ AI để tránh spam / race condition. */
    private void setSending(boolean sending) {
        pbAiThinking.setVisibility(sending ? View.VISIBLE : View.GONE);
        btnSendChat.setEnabled(!sending);
        btnSendChat.setAlpha(sending ? 0.5f : 1f);
        etChatInput.setEnabled(!sending);
    }

    private void scheduleFinish() {
        mainHandler.postDelayed(this::finish, FINISH_DELAY_MS);
    }
}
