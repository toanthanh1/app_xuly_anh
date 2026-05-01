package com.example.app_xhinh_anh.features.ai_assistant.data;

import com.example.app_xhinh_anh.features.ai_assistant.domain.PromptProvider;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.ai.client.generativeai.type.RequestOptions;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiApiClient {
    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public GeminiApiClient(String apiKey) {
        // Sử dụng gemini-2.5-flash theo yêu cầu của bạn
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                apiKey,
                null, // generationConfig
                null, // safetySettings
                new RequestOptions(30000L, "v1"),
                null, // tools
                null, // toolConfig
                null  // systemInstruction
        );
        this.model = GenerativeModelFutures.from(gm);
    }

    public interface AiCallback {
        void onSuccess(String response);
        void onError(Throwable t);
    }

    public void sendMessage(String userPrompt, AiCallback callback) {
        String fullPrompt = PromptProvider.getSystemPrompt() + "\n\nNgười dùng: " + userPrompt;
        
        Content userContent = new Content.Builder()
                .addText(fullPrompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(userContent);
        
        response.addListener(() -> {
            try {
                GenerateContentResponse result = response.get();
                String text = result.getText();
                if (text != null) {
                    callback.onSuccess(text);
                } else {
                    callback.onError(new Exception("AI returned empty response"));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }, executor);
    }
}
