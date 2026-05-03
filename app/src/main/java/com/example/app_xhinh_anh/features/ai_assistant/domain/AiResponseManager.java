package com.example.app_xhinh_anh.features.ai_assistant.domain;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Điều phối phản hồi của trợ lý AI:
 * 1) Bắt từ khóa local cho các yêu cầu phổ biến (không cần gọi API).
 * 2) Parse JSON Gemini trả về thành các action cụ thể (APPLY_FILTER, ADJUST với
 *    nhiều thuộc tính, OPEN_TOOL, REMOVE_BACKGROUND, MESSAGE).
 *
 * Toàn bộ các action đều đi qua {@link ResponseCallback} để Activity ánh xạ
 * sang Intent / hành động UI tương ứng.
 */
public class AiResponseManager {

    public interface ResponseCallback {
        void onMessage(String text);
        void onApplyFilter(String filterName);
        /** Hỗ trợ nhiều thuộc tính cùng lúc — list rỗng không được gọi. */
        void onAdjustments(List<ActionMapper.Adjustment> adjustments);
        void onOpenTool(String toolName);
        void onRemoveBackground();
        void onError(String error);
    }

    /**
     * Bắt nhanh các yêu cầu thường gặp bằng từ khóa, tránh gọi API. Trả về true nếu
     * đã match — caller dừng pipeline. Tất cả các nhánh đều trả lời người dùng (onMessage)
     * trước khi gọi action để chat hiển thị mượt.
     */
    public static boolean handleLocalInput(String input, ResponseCallback callback) {
        if (input == null || input.isEmpty()) return false;
        String s = input.toLowerCase().trim();

        if (s.contains("chào") || s.contains("hi ") || s.equals("hi")
                || s.contains("hello") || s.contains("helo")) {
            callback.onMessage("Xin chào! Tôi có thể giúp bạn:\n"
                    + "✨ Làm trắng da\n📜 Áp dụng bộ lọc Hoài cổ\n☀️ Chỉnh ảnh tươi sáng hơn");
            return true;
        }

        if (s.contains("trắng") || s.contains("sáng da")) {
            callback.onMessage("✨ Đang kích hoạt chế độ làm đẹp da (Snow White)...");
            callback.onApplyFilter("Snow White");
            return true;
        }

        if (s.contains("hoài cổ") || s.contains("retro") || s.contains("cũ") || s.contains("sepia")) {
            callback.onMessage("📜 Đang áp dụng phong cách hoài cổ (Sepia)...");
            callback.onApplyFilter("Sepia");
            return true;
        }

        if (s.contains("tươi sáng") || s.contains("sáng hơn") || s.contains("vivid")) {
            callback.onMessage("☀️ Đang làm bức ảnh tươi sáng hơn...");
            callback.onApplyFilter("Vivid");
            return true;
        }

        if (s.contains("xóa phông") || s.contains("xóa nền") || s.contains("xoá phông") || s.contains("xoá nền")) {
            callback.onMessage("🧹 Đang xóa phông nền...");
            callback.onRemoveBackground();
            return true;
        }

        if (s.contains("tăng sáng") || s.contains("thêm sáng") || s.contains("sáng thêm")) {
            callback.onMessage("☀️ Đã tăng độ sáng thêm 20%");
            callback.onAdjustments(Collections.singletonList(new ActionMapper.Adjustment("brightness", 20)));
            return true;
        }
        if (s.contains("giảm sáng") || s.contains("tối đi")) {
            callback.onMessage("🌙 Đã giảm độ sáng đi 20%");
            callback.onAdjustments(Collections.singletonList(new ActionMapper.Adjustment("brightness", -20)));
            return true;
        }

        if (s.contains("tăng tương phản") || s.contains("đậm hơn")) {
            callback.onMessage("🎨 Đã tăng độ tương phản");
            callback.onAdjustments(Collections.singletonList(new ActionMapper.Adjustment("contrast", 20)));
            return true;
        }
        if (s.contains("giảm tương phản") || s.contains("nhạt hơn")) {
            callback.onMessage("🌫️ Đã giảm độ tương phản");
            callback.onAdjustments(Collections.singletonList(new ActionMapper.Adjustment("contrast", -20)));
            return true;
        }

        return false;
    }

    /**
     * Parse JSON do Gemini trả về. Bao dung với:
     * - Chuỗi có chèn markdown code-block ```json ... ```
     * - Chuỗi có text ngoài cặp ngoặc {}
     * - Schema mới (adjustments[]) lẫn schema cũ (property/value đơn).
     */
    public static void parseResponse(String rawResponse, ResponseCallback callback) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            callback.onMessage("⚠️ AI không phản hồi.");
            return;
        }
        try {
            int firstBrace = rawResponse.indexOf('{');
            int lastBrace = rawResponse.lastIndexOf('}');
            if (firstBrace < 0 || lastBrace <= firstBrace) {
                // Không có JSON — trả về text gốc (đã loại bỏ code-block)
                callback.onMessage(stripCodeFences(rawResponse));
                return;
            }
            String jsonStr = rawResponse.substring(firstBrace, lastBrace + 1);
            JSONObject json = new JSONObject(jsonStr);
            String action = json.optString("action", "").toUpperCase();

            switch (action) {
                case "APPLY_FILTER": {
                    String filter = json.optString("filter_name", "").trim();
                    if (filter.isEmpty()) {
                        callback.onMessage("⚠️ AI không nêu rõ tên bộ lọc.");
                    } else {
                        callback.onMessage("✅ Đã chọn bộ lọc: " + filter);
                        callback.onApplyFilter(filter);
                    }
                    break;
                }
                case "ADJUST": {
                    List<ActionMapper.Adjustment> adjustments = new ArrayList<>();
                    if (json.has("adjustments")) {
                        JSONArray array = json.getJSONArray("adjustments");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            String prop = obj.optString("property", "").trim();
                            if (prop.isEmpty()) continue;
                            adjustments.add(new ActionMapper.Adjustment(prop, obj.optInt("value", 0)));
                        }
                    } else if (json.has("property")) {
                        String prop = json.optString("property", "").trim();
                        if (!prop.isEmpty()) {
                            adjustments.add(new ActionMapper.Adjustment(prop, json.optInt("value", 0)));
                        }
                    }
                    if (adjustments.isEmpty()) {
                        callback.onMessage("⚠️ AI không nêu rõ thông số chỉnh.");
                    } else {
                        StringBuilder sb = new StringBuilder("✅ Đã chỉnh: ");
                        for (int i = 0; i < adjustments.size(); i++) {
                            if (i > 0) sb.append(", ");
                            ActionMapper.Adjustment a = adjustments.get(i);
                            sb.append(a.property).append("=").append(a.value);
                        }
                        callback.onMessage(sb.toString());
                        callback.onAdjustments(adjustments);
                    }
                    break;
                }
                case "OPEN_TOOL": {
                    String tool = json.optString("tool_name", "").trim();
                    if (tool.isEmpty()) {
                        callback.onMessage("⚠️ AI không nêu rõ công cụ cần mở.");
                    } else {
                        callback.onMessage("🛠 Đang mở công cụ: " + tool);
                        callback.onOpenTool(tool);
                    }
                    break;
                }
                case "REMOVE_BACKGROUND": {
                    callback.onMessage("🧹 Đang xóa phông nền...");
                    callback.onRemoveBackground();
                    break;
                }
                case "MESSAGE": {
                    callback.onMessage(json.optString("message", stripCodeFences(rawResponse)));
                    break;
                }
                default:
                    // Hành động không xác định — trả lại nguyên văn để user thấy
                    callback.onMessage(stripCodeFences(rawResponse));
                    break;
            }
        } catch (Exception e) {
            // JSON hỏng — fallback hiển thị text
            callback.onMessage(stripCodeFences(rawResponse));
        }
    }

    private static String stripCodeFences(String s) {
        return s.replaceAll("(?s)```json(.*?)```", "$1").replaceAll("```", "").trim();
    }
}
