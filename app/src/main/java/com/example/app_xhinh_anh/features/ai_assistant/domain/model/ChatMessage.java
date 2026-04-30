package com.example.app_xhinh_anh.features.ai_assistant.domain.model;

import android.graphics.Bitmap;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private Bitmap image;

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    public ChatMessage(String message, boolean isUser, Bitmap image) {
        this.message = message;
        this.isUser = isUser;
        this.image = image;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    public Bitmap getImage() {
        return image;
    }
}
