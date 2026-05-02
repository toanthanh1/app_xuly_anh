package com.example.app_xhinh_anh.features.ai_assistant.domain;

import org.json.JSONObject;

public class ActionMapper {
    public interface ActionListener {
        void onApplyFilter(String filterName);
        void onAdjustProperty(String property, int value);
        void onOpenTool(String toolName);
        void onRemoveBackground();
        void onMessage(String message);
    }

    public static void map(String aiResponse, ActionListener listener) {
        try {
            String jsonStr = aiResponse;
            
            // Tìm vị trí của JSON trong chuỗi (giữa cặp ngoặc { })
            int firstBrace = aiResponse.indexOf('{');
            int lastBrace = aiResponse.lastIndexOf('}');
            
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                jsonStr = aiResponse.substring(firstBrace, lastBrace + 1);
            } else {
                // Nếu không thấy dấu ngoặc, có thể AI trả về text thuần
                listener.onMessage(aiResponse);
                return;
            }

            JSONObject json = new JSONObject(jsonStr);
            String action = json.optString("action", "");

            switch (action) {
                case "APPLY_FILTER":
                    listener.onApplyFilter(json.optString("filter_name"));
                    break;
                case "ADJUST":
                    listener.onAdjustProperty(json.optString("property"), json.optInt("value", 0));
                    break;
                case "OPEN_TOOL":
                    listener.onOpenTool(json.optString("tool_name"));
                    break;
                case "REMOVE_BACKGROUND":
                    listener.onRemoveBackground();
                    break;
                case "MESSAGE":
                default:
                    listener.onMessage(json.optString("message", aiResponse));
                    break;
            }
        } catch (Exception e) {
            // If not JSON or parsing fails, treat as a plain message
            listener.onMessage(aiResponse);
        }
    }
}
