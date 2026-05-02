package com.example.app_xhinh_anh.features.ai_assistant.domain;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ActionMapper {
    public static class Adjustment {
        public String property;
        public int value;

        public Adjustment(String property, int value) {
            this.property = property;
            this.value = value;
        }
    }

    public interface ActionListener {
        void onApplyFilter(String filterName);
        void onAdjustProperties(List<Adjustment> adjustments);
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
                    List<Adjustment> adjustments = new ArrayList<>();
                    if (json.has("adjustments")) {
                        JSONArray array = json.getJSONArray("adjustments");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            adjustments.add(new Adjustment(obj.optString("property"), obj.optInt("value", 0)));
                        }
                    } else if (json.has("property")) {
                        adjustments.add(new Adjustment(json.optString("property"), json.optInt("value", 0)));
                    }
                    listener.onAdjustProperties(adjustments);
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
