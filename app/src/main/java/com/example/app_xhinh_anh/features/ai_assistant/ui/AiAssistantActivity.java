package com.example.app_xhinh_anh.features.ai_assistant.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.example.app_xhinh_anh.features.ai_assistant.data.GeminiImageClient;
import com.example.app_xhinh_anh.features.ai_assistant.domain.ActionMapper;
import com.example.app_xhinh_anh.features.ai_assistant.domain.AiResponseManager;
import com.example.app_xhinh_anh.features.ai_assistant.domain.model.ChatMessage;
import com.example.app_xhinh_anh.features.ai_assistant.ui.adapter.ChatAdapter;

import java.io.File;
import java.io.FileOutputStream;
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
    private GeminiImageClient geminiImageClient;
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
        public void onGenerateImage(String prompt) {
            if (geminiImageClient == null) {
                onMessage("⚠️ Tính năng tạo ảnh cần GEMINI_API_KEY.");
                return;
            }
            runOnUiThread(() -> setSending(true));
            geminiImageClient.generate(prompt, new GeminiImageClient.ImageCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    runOnUiThread(() -> {
                        setSending(false);
                        Runnable openInEditor = () -> handoffGeneratedImage(bitmap);
                        chatAdapter.addMessage(new ChatMessage(
                                "✅ Đã tạo ảnh. Nhấn vào ảnh để mở trong editor và chỉnh sửa tiếp.",
                                false, bitmap, openInEditor));
                        rvChatHistory.scrollToPosition(chatAdapter.getItemCount() - 1);
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> {
                        setSending(false);
                        onMessage("❌ Không tạo được ảnh: "
                                + (t != null ? t.getMessage() : "lỗi không xác định"));
                    });
                }
            });
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
            geminiImageClient = new GeminiImageClient(apiKey);
        } else {
            chatAdapter.addMessage(new ChatMessage(
                    "⚠️ Chưa cấu hình GEMINI_API_KEY trong local.properties.\n"
                            + "Chỉ các từ khóa local hoạt động (vd: \"tăng sáng\", \"hoài cổ\", \"xóa nền\").",
                    false));
        }

        if (chatAdapter.getItemCount() == 0
                || (chatAdapter.getItemCount() == 1 && apiKey != null && !apiKey.isEmpty())) {
            chatAdapter.addMessage(new ChatMessage("👋 Chào bạn! Tôi là Trợ lý AI của App Xhinh Anh.\n\n" +
                    "💡 **Bạn có thể thử:**\n\n" +
                    "🎨 **Bộ lọc & chỉnh ảnh:**\n" +
                    "• \"Làm trắng da\"  • \"Phong cách Polaroid\"\n" +
                    "• \"Tăng sáng 30%\"  • \"Làm rõ chi tiết, sắc nét\"\n\n" +
                    "✂️ **Công cụ:** \"Xóa nền\"  • \"Mở Curves\"  • \"Cắt ảnh\"\n\n" +
                    "🪄 **Tạo ảnh AI mới (như Midjourney/DALL-E):**\n" +
                    "• \"Tạo ảnh con mèo phi hành gia trong vũ trụ\"\n" +
                    "• \"Vẽ cảnh hoàng hôn trên biển, phong cách anime\"\n" +
                    "Sau khi AI tạo xong, nhấn vào ảnh để mở trong editor.\n\n" +
                    "Bạn muốn làm gì?", false));
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

        // Có Gemini → đi thẳng vào API. Local matcher chỉ là fallback offline để
        // tránh false-positive (vd "chi tiết" khớp nhầm "hi " greeting).
        if (geminiApiClient != null) {
            sendToGemini(message);
            return;
        }

        if (AiResponseManager.handleLocalInput(message, aiCallback)) {
            return;
        }
        aiCallback.onMessage("⚠️ Câu lệnh này cần API key. Hãy cấu hình GEMINI_API_KEY hoặc thử các từ khóa local (vd: \"tăng sáng\", \"hoài cổ\", \"xóa nền\").");
    }

    private void sendToGemini(String message) {
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

    /**
     * Lưu bitmap AI sinh ra vào cache rồi gửi path về EditorActivity. Dùng đường dẫn
     * file thay vì FileProvider URI vì cùng process — tránh boilerplate authority.
     */
    private void handoffGeneratedImage(Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            File f = new File(getCacheDir(), "ai_gen_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream out = new FileOutputStream(f)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Intent r = new Intent();
            r.putExtra("action", "GENERATE_IMAGE");
            r.putExtra("image_path", f.getAbsolutePath());
            setResult(RESULT_OK, r);
            finish();
        } catch (Exception e) {
            aiCallback.onMessage("❌ Không lưu được ảnh: " + e.getMessage());
        }
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
