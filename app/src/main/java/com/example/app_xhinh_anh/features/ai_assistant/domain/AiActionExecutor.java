package com.example.app_xhinh_anh.features.ai_assistant.domain;

import android.os.Handler;
import android.os.Looper;

/**
 * Class này chịu trách nhiệm thực thi các hành động mà AI yêu cầu.
 * Giúp tách biệt logic khỏi Activity chính để dễ quản lý và bảo trì.
 */
public class AiActionExecutor {

    public interface EditorActions {
        void applyFilter(String filterName);
        void adjustProperty(String property, int value);
        void openTool(String toolName);
        void removeBackground();
        void addChatMessage(String message, boolean isUser);
        void showThinking(boolean show);
    }

    private final EditorActions actions;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AiActionExecutor(EditorActions actions) {
        this.actions = actions;
    }

    public void executeResponse(String aiResponse) {
        mainHandler.post(() -> actions.showThinking(false));

        ActionMapper.map(aiResponse, new ActionMapper.ActionListener() {
            @Override
            public void onApplyFilter(String filterName) {
                mainHandler.post(() -> {
                    actions.addChatMessage("Đang áp dụng bộ lọc: " + filterName, false);
                    actions.applyFilter(filterName);
                });
            }

            @Override
            public void onAdjustProperty(String property, int value) {
                mainHandler.post(() -> {
                    actions.addChatMessage("Đang chỉnh " + property + " thành " + value + "%", false);
                    actions.adjustProperty(property, value);
                });
            }

            @Override
            public void onOpenTool(String toolName) {
                mainHandler.post(() -> {
                    actions.addChatMessage("Đang mở công cụ: " + toolName, false);
                    actions.openTool(toolName);
                });
            }

            @Override
            public void onRemoveBackground() {
                mainHandler.post(() -> {
                    actions.addChatMessage("Đang thực hiện xóa nền...", false);
                    actions.removeBackground();
                });
            }

            @Override
            public void onMessage(String message) {
                mainHandler.post(() -> actions.addChatMessage(message, false));
            }
        });
    }
}
