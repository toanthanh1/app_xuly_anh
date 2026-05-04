package com.example.app_xhinh_anh.features.ai_assistant.domain;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

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
        /** Sinh ảnh mới từ prompt (gọi Imagen-style model, hiển thị bubble ảnh). */
        void onGenerateImage(String prompt);
        void onError(String error);
    }

    /**
     * Bắt nhanh các yêu cầu thường gặp khi KHÔNG có Gemini (offline). Có Gemini thì
     * Activity bỏ qua bước này — Gemini hiểu ngữ cảnh chính xác hơn nhiều và tránh
     * false-positive kiểu "chi tiết" → "hi " (greeting).
     *
     * Tất cả lookup đều dùng word-boundary regex để tránh khớp giữa từ.
     */
    public static boolean handleLocalInput(String input, ResponseCallback callback) {
        if (input == null || input.isEmpty()) return false;
        String s = input.toLowerCase().trim();

        // Greeting — chỉ khớp khi "hi"/"hello" đứng riêng, không phải substring
        if (matches(s, "chào", "hello", "helo") || s.equals("hi") || s.startsWith("hi ")) {
            callback.onMessage("Xin chào! Tôi có thể giúp bạn:\n"
                    + "✨ Làm trắng da\n📜 Áp dụng bộ lọc Hoài cổ\n☀️ Chỉnh ảnh tươi sáng hơn\n"
                    + "🔍 Làm rõ chi tiết / sắc nét\n🎨 Tạo ảnh mới (vd: \"tạo ảnh con mèo phi hành gia\")");
            return true;
        }

        if (matches(s, "trắng da", "sáng da", "làm trắng")) {
            callback.onMessage("✨ Đang kích hoạt chế độ làm đẹp da (Snow White)...");
            callback.onApplyFilter("Snow White");
            return true;
        }

        if (matches(s, "hoài cổ", "retro", "sepia")) {
            callback.onMessage("📜 Đang áp dụng phong cách hoài cổ (Sepia)...");
            callback.onApplyFilter("Sepia");
            return true;
        }

        if (matches(s, "tươi sáng", "sáng hơn", "vivid", "rực rỡ")) {
            callback.onMessage("☀️ Đang làm bức ảnh tươi sáng hơn...");
            callback.onApplyFilter("Vivid");
            return true;
        }

        if (matches(s, "xóa phông", "xóa nền", "xoá phông", "xoá nền", "remove background")) {
            callback.onMessage("🧹 Đang xóa phông nền...");
            callback.onRemoveBackground();
            return true;
        }

        if (matches(s, "tăng sáng", "thêm sáng", "sáng thêm")) {
            callback.onMessage("☀️ Đã tăng độ sáng thêm 20%");
            callback.onAdjustments(Collections.singletonList(new ActionMapper.Adjustment("brightness", 20)));
            return true;
        }
        if (matches(s, "giảm sáng", "tối đi", "tối hơn")) {
            callback.onMessage("🌙 Đã giảm độ sáng đi 20%");
            callback.onAdjustments(Collections.singletonList(new ActionMapper.Adjustment("brightness", -20)));
            return true;
        }

        if (matches(s, "tăng tương phản", "đậm hơn")) {
            callback.onMessage("🎨 Đã tăng độ tương phản");
            callback.onAdjustments(Collections.singletonList(new ActionMapper.Adjustment("contrast", 20)));
            return true;
        }
        if (matches(s, "giảm tương phản", "nhạt hơn")) {
            callback.onMessage("🌫️ Đã giảm độ tương phản");
            callback.onAdjustments(Collections.singletonList(new ActionMapper.Adjustment("contrast", -20)));
            return true;
        }

        if (matches(s, "làm rõ", "rõ nét", "chi tiết", "sắc nét", "sharpen", "clarity")) {
            callback.onMessage("🔍 Đã làm rõ chi tiết (clarity +30, sharpness +20)");
            List<ActionMapper.Adjustment> a = new ArrayList<>();
            a.add(new ActionMapper.Adjustment("clarity", 30));
            a.add(new ActionMapper.Adjustment("sharpness", 20));
            callback.onAdjustments(a);
            return true;
        }

        return false;
    }

    /** Khớp khi {@code phrase} xuất hiện như cụm từ độc lập trong {@code s}. */
    private static boolean matches(String s, String... phrases) {
        for (String p : phrases) {
            if (containsPhrase(s, p)) return true;
        }
        return false;
    }

    private static boolean containsPhrase(String s, String phrase) {
        // Cụm có dấu cách: contains() là đủ vì rất khó false-positive (vd "tăng sáng").
        if (phrase.contains(" ")) return s.contains(phrase);
        // Từ đơn: dùng word-boundary để tránh khớp giữa từ.
        // Pattern \b không hoạt động tốt với tiếng Việt có dấu, nên dùng (?<=^|\W)...(?=\W|$).
        return Pattern.compile("(?:^|\\W)" + Pattern.quote(phrase) + "(?:\\W|$)",
                Pattern.UNICODE_CASE).matcher(s).find();
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
                case "GENERATE_IMAGE": {
                    String prompt = json.optString("prompt", "").trim();
                    if (prompt.isEmpty()) {
                        callback.onMessage("⚠️ AI không nêu rõ nội dung ảnh muốn tạo.");
                    } else {
                        callback.onMessage("🎨 Đang tạo ảnh: \"" + prompt + "\"...");
                        callback.onGenerateImage(prompt);
                    }
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
