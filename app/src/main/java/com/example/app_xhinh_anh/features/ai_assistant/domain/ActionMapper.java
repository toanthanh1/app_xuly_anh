package com.example.app_xhinh_anh.features.ai_assistant.domain;

import org.json.JSONObject;

public class ActionMapper {
    public interface ActionListener {
        void onApplyFilter(String filterName);
        void onAdjustProperty(String property, int value);
        void onMessage(String message);
    }

    public static void map(String aiResponse, ActionListener listener) {
        try {
            String jsonStr = aiResponse;
            // Tìm vị trí của dấu { đầu tiên và } cuối cùng để trích xuất JSON
            int start = aiResponse.indexOf("{");
            int end = aiResponse.lastIndexOf("}");
            
            if (start != -1 && end != -1 && end > start) {
                jsonStr = aiResponse.substring(start, end + 1);
            }

            JSONObject json = new JSONObject(jsonStr);
            String action = json.optString("action", "");

            switch (action) {
                case "APPLY_FILTER":
                    listener.onApplyFilter(json.optString("filter_name"));
                    break;
                case "ADJUST":
                    listener.onAdjustProperty(json.optString("property"), json.optInt("value", 50));
                    break;
                case "MESSAGE":
                default:
                    listener.onMessage(json.optString("message", aiResponse));
                    break;
            }
        } catch (Exception e) {
            // Nếu không tìm thấy JSON hoặc lỗi, coi như là tin nhắn văn bản
            listener.onMessage(aiResponse);
        }
    }
}
