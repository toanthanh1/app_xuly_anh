package com.example.app_xhinh_anh.features.ai_assistant.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Sinh ảnh qua REST API Gemini Image (model {@code gemini-2.5-flash-image-preview}).
 *
 * Lý do dùng REST trực tiếp thay vì SDK {@code com.google.ai.client.generativeai}:
 * SDK Java hiện chưa expose {@code inlineData} (image bytes) trong response một cách tin cậy
 * — gọi REST cho ta toàn quyền parse.
 *
 * KHÔNG đụng vào {@link GeminiApiClient} (text-only) theo yêu cầu cấm thay đổi model
 * gemini-2.5-flash hiện có.
 */
public class GeminiImageClient {

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image-preview:generateContent";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final OkHttpClient http;

    public GeminiImageClient(String apiKey) {
        this.apiKey = apiKey;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public interface ImageCallback {
        void onSuccess(Bitmap bitmap);
        void onError(Throwable t);
    }

    public void generate(String prompt, ImageCallback callback) {
        if (prompt == null || prompt.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("prompt rỗng"));
            return;
        }
        String body;
        try {
            body = new JSONObject()
                    .put("contents", new JSONArray()
                            .put(new JSONObject()
                                    .put("parts", new JSONArray()
                                            .put(new JSONObject().put("text", prompt)))))
                    .toString();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        Request request = new Request.Builder()
                .url(ENDPOINT + "?key=" + apiKey)
                .post(RequestBody.create(body, JSON))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    String text = rb != null ? rb.string() : "";
                    if (!response.isSuccessful()) {
                        callback.onError(new IOException("HTTP " + response.code() + ": " + text));
                        return;
                    }
                    Bitmap bm = extractBitmap(text);
                    if (bm == null) {
                        callback.onError(new IOException("Phản hồi không có dữ liệu ảnh"));
                    } else {
                        callback.onSuccess(bm);
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    /** Tìm part đầu tiên có {@code inlineData.data} là PNG/JPEG base64. */
    private static Bitmap extractBitmap(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null) return null;
        for (int i = 0; i < candidates.length(); i++) {
            JSONObject content = candidates.getJSONObject(i).optJSONObject("content");
            if (content == null) continue;
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null) continue;
            for (int j = 0; j < parts.length(); j++) {
                JSONObject inline = parts.getJSONObject(j).optJSONObject("inlineData");
                if (inline == null) inline = parts.getJSONObject(j).optJSONObject("inline_data");
                if (inline == null) continue;
                String b64 = inline.optString("data", "");
                if (b64.isEmpty()) continue;
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }
        return null;
    }
}
