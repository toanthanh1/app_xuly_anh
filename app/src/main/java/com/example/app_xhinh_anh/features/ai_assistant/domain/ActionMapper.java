package com.example.app_xhinh_anh.features.ai_assistant.domain;

import org.json.JSONObject;

public class ActionMapper {
    public interface ActionListener {
        void onApplyFilter(String filterName);
        void onAdjustProperty(String property, int value);
        void onRemoveBackground();
        void onMessage(String message);
    }

    public static void map(String aiResponse, ActionListener listener) {
        try {
            // Remove markdown code blocks if present
            String cleanedResponse = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            JSONObject json = new JSONObject(cleanedResponse);
            String action = json.optString("action", "");

            switch (action) {
                case "APPLY_FILTER":
                    listener.onApplyFilter(json.optString("filter_name"));
                    break;
                case "ADJUST":
                    listener.onAdjustProperty(json.optString("property"), json.optInt("value", 50));
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
