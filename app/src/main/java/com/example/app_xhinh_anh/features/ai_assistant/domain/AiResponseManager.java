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
     * XỬ LÝ ĐẦU VÀO: Giảm tải cho AI bằng cách nhận diện các lệnh phổ biến tại local.
     * @return true nếu lệnh đã được xử lý, không cần gửi lên server.
     */
    public static boolean handleLocalInput(String input, ResponseCallback callback) {
        String lowerInput = input.toLowerCase().trim();
        
        // 1. Phân loại các yêu cầu chào hỏi
        if (lowerInput.matches("^(xin chào|hi|hello|chào|helo)$")) {
            callback.onMessage("Xin chào! Tôi là Trợ lý AI của bạn. Tôi có thể giúp bạn chọn bộ lọc màu hoặc chỉnh độ sáng ảnh. Bạn muốn thử gì nào?");
            return true;
        }

        // 2. Phân loại các yêu cầu cụ thể đã biết (Hard-coded để tiết kiệm API)
        if (lowerInput.contains("làm trắng") || lowerInput.contains("trắng da") || lowerInput.contains("da sáng")) {
            callback.onApplyFilter("Snow White");
            callback.onMessage("✨ Đang kích hoạt chế độ làm đẹp da (Snow White) cho bạn...");
            return true;
        }

        if (lowerInput.contains("hoài cổ") || lowerInput.contains("ảnh cũ")) {
            callback.onApplyFilter("Sepia");
            callback.onMessage("📜 Đang áp dụng phong cách hoài cổ (Sepia) cho bức ảnh...");
            return true;
        }

        return false; // Chuyển tiếp cho AI xử lý nếu không khớp lệnh local
    }

    /**
     * XỬ LÝ ĐẦU RA: Phân tích kết quả từ AI để hiển thị giao diện đẹp hơn thay vì hiện JSON.
     */
    public static void parseResponse(String rawResponse, ResponseCallback callback) {
        try {
            // Loại bỏ các phần text thừa nếu AI trả về định dạng ```json ... ```
            String cleanResponse = rawResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            
            int jsonStart = cleanResponse.indexOf("{");
            int jsonEnd = cleanResponse.lastIndexOf("}");
            
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonStr = cleanResponse.substring(jsonStart, jsonEnd + 1);
                JSONObject json = new JSONObject(jsonStr);
                String action = json.optString("action", "");

                switch (action) {
                    case "APPLY_FILTER":
                        String filterName = json.optString("filter_name", "");
                        callback.onApplyFilter(filterName);
                        callback.onMessage("✅ Đã áp dụng bộ lọc: " + filterName);
                        break;
                        
                    case "ADJUST":
                        callback.onMessage("⚙️ Đã điều chỉnh các thông số ảnh theo yêu cầu của bạn.");
                        break;
                        
                    case "MESSAGE":
                        callback.onMessage(json.optString("message", ""));
                        break;
                        
                    default:
                        callback.onMessage(cleanResponse);
                }
            } else {
                callback.onMessage(rawResponse);
            }
        } catch (Exception e) {
            callback.onMessage(rawResponse);
        }
    }
}
