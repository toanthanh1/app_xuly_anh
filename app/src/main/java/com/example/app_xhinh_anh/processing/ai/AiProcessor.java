package com.example.app_xhinh_anh.processing.ai;

import android.graphics.Bitmap;

public interface AiProcessor {
    interface AiCallback {
        void onSuccess(Bitmap result);
        void onError(Throwable e);
    }
    void process(Bitmap input, AiCallback callback);
}
