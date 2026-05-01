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
     * TỐI ƯU ĐẦU VÀO: Phản hồi tức thì dựa trên từ khóa quan trọng.
     * Không sử dụng Regex phức tạp để đảm bảo tốc độ xử lý local cao nhất.
     */
    public static boolean handleLocalInput(String input, ResponseCallback callback) {
        if (input == null || input.isEmpty()) return false;
        String s = input.toLowerCase().trim();
        
        // 1. Chào hỏi linh hoạt (Chỉ cần chứa từ chào, hi, hello)
        if (s.contains("chào") || s.contains("hi") || s.contains("hello") || s.contains("helo")) {
            callback.onMessage("Xin chào! Tôi có thể giúp bạn:\n✨ Làm trắng da\n📜 Áp dụng bộ lọc Hoài cổ\n☀️ Chỉnh ảnh tươi sáng hơn");
            return true;
        }

        // 2. Lệnh làm trắng/sáng da (Linh hoạt theo từ khóa)
        if (s.contains("trắng") || s.contains("sáng da")) {
            callback.onMessage("✨ Đang kích hoạt chế độ làm đẹp da (Snow White)...");
            callback.onApplyFilter("Snow White");
            return true;
        }

        // 3. Lệnh hoài cổ/retro/sepia/cũ
        if (s.contains("hoài cổ") || s.contains("retro") || s.contains("cũ") || s.contains("sepia")) {
            callback.onMessage("📜 Đang áp dụng phong cách hoài cổ (Sepia)...");
            callback.onApplyFilter("Sepia");
            return true;
        }
        
        // 4. Lệnh rực rỡ/sáng hơn
        if (s.contains("tươi sáng") || s.contains("sáng hơn") || s.contains("vivid")) {
            callback.onMessage("☀️ Đang làm bức ảnh tươi sáng hơn...");
            callback.onApplyFilter("Vivid");
            return true;
        }

        // 5. Tăng/Giảm độ sáng (Brightness)
        if (s.contains("tăng sáng") || s.contains("thêm sáng") || s.contains("sáng thêm")) {
            callback.onMessage("☀️ Đã tăng độ sáng thêm 20%");
            callback.onAdjust("brightness", 20);
            return true;
        }
        if (s.contains("giảm sáng") || s.contains("tối đi")) {
            callback.onMessage("🌙 Đã giảm độ sáng đi 20%");
            callback.onAdjust("brightness", -20);
            return true;
        }

        // 6. Tăng/Giảm tương phản (Contrast)
        if (s.contains("tăng tương phản") || s.contains("đậm hơn")) {
            callback.onMessage("🎨 Đã tăng độ tương phản");
            callback.onAdjust("contrast", 20);
            return true;
        }
        if (s.contains("giảm tương phản") || s.contains("nhạt hơn")) {
            callback.onMessage("🌫️ Đã giảm độ tương phản");
            callback.onAdjust("contrast", -20);
            return true;
        }

        return false; // Nếu không khớp từ khóa local thì mới gửi lên AI xử lý
    }

    /**
     * TỐI ƯU ĐẦU RA: Parse JSON cực nhanh và làm sạch text.
     */
    public static void parseResponse(String rawResponse, ResponseCallback callback) {
        try {
            // Loại bỏ markdown nếu AI trả về định dạng code block
            String clean = rawResponse.replaceAll("(?s)```json(.*?)```", "$1").replaceAll("```", "").trim();
            
            if (clean.startsWith("{")) {
                JSONObject json = new JSONObject(clean);
                String action = json.optString("action", "");
                if ("APPLY_FILTER".equals(action)) {
                    String filter = json.optString("filter_name", "");
                    callback.onApplyFilter(filter);
                    callback.onMessage("✅ Đã chọn bộ lọc: " + filter);
                } else if ("ADJUST".equals(action)) {
                    String property = json.optString("property", "");
                    int value = json.optInt("value", 0);
                    callback.onAdjust(property, value);
                    callback.onMessage("✅ Đã chỉnh " + property + " thành " + value);
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
