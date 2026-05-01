package com.example.app_xhinh_anh.features.ai_assistant.domain;

import org.json.JSONObject;

public class AiResponseManager {

    public interface ResponseCallback {
        void onMessage(String text);
        void onApplyFilter(String filterName);
        void onAdjust(String property, int value);
        void onError(String error);
    }

    /**
     * TỐI ƯU ĐẦU VÀO: Phản ứng tức thì với các lệnh phổ biến
     */
    public static boolean handleLocalInput(String input, ResponseCallback callback) {
        if (input == null) return false;
        String lowerInput = input.toLowerCase().trim();
        
        // Chào hỏi - Phản hồi cực nhanh
        if (lowerInput.equals("hi") || lowerInput.equals("hello") || lowerInput.equals("xin chào") || lowerInput.equals("chào")) {
            callback.onMessage("Xin chào! Bạn cần tôi giúp gì cho bức ảnh này không? (Ví dụ: 'Làm trắng da', 'Chỉnh ảnh hoài cổ')");
            return true;
        }

        // Lệnh làm trắng
        if (lowerInput.contains("làm trắng") || lowerInput.contains("trắng da") || lowerInput.contains("sáng da")) {
            callback.onMessage("✨ Đang làm trắng da cho bạn...");
            callback.onApplyFilter("Snow White");
            return true;
        }

        // Lệnh hoài cổ
        if (lowerInput.contains("hoài cổ") || lowerInput.contains("ảnh cũ") || lowerInput.contains("retro")) {
            callback.onMessage("📜 Đang áp dụng phong cách hoài cổ...");
            callback.onApplyFilter("Sepia");
            return true;
        }

        return false;
    }

    /**
     * TỐI ƯU ĐẦU RA: Xử lý chuỗi JSON từ AI một cách gọn gàng
     */
    public static void parseResponse(String rawResponse, ResponseCallback callback) {
        try {
            // Loại bỏ các ký tự thừa từ AI (như markdown ```json)
            String clean = rawResponse.replace("```json", "").replace("```", "").trim();
            
            if (clean.startsWith("{")) {
                JSONObject json = new JSONObject(clean);
                String action = json.optString("action", "");

                if ("APPLY_FILTER".equals(action)) {
                    String filter = json.optString("filter_name", "");
                    callback.onMessage("✅ Đã chọn bộ lọc: " + filter);
                    callback.onApplyFilter(filter);
                } else if ("MESSAGE".equals(action)) {
                    callback.onMessage(json.optString("message", ""));
                } else {
                    callback.onMessage(clean);
                }
            } else {
                callback.onMessage(rawResponse);
            }
        } catch (Exception e) {
            callback.onMessage(rawResponse);
        }
    }
}
