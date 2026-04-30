# Roadmap: Trợ lý AI Chỉnh Ảnh bằng Văn Bản (Chat UI)

Tài liệu này phác thảo lộ trình xây dựng tính năng "Conversational UI" (Khung chat AI), cho phép người dùng ra lệnh bằng văn bản để chỉnh sửa hình ảnh. Lộ trình được chia làm 2 giai đoạn (Phase) từ cơ bản đến nâng cao.

---

## Phase 1: AI Điều phối Lệnh cơ bản (Action Mapping)

**Mục tiêu:** Xây dựng hệ thống chat cơ bản, nơi AI đóng vai trò như một "người trực tổng đài", lắng nghe yêu cầu của người dùng và tự động bấm hộ các nút Filter đã có sẵn trong app.

### 1. Tích hợp Giao diện (UI)
- Mở `activity_editor.xml`.
- Thêm một khu vực Chat ở cạnh dưới (Bottom) của màn hình, bao gồm:
  - Một `EditText` để người dùng nhập yêu cầu (VD: "Làm cho bức ảnh này cũ đi").
  - Một `ImageButton` (icon Send) để gửi lệnh.
  - Một `ProgressBar` nhỏ hiển thị trạng thái "AI đang suy nghĩ...".

### 2. Tích hợp API Ngôn ngữ (LLM Integration)
- Khuyến nghị sử dụng **Google Gemini API** (nhẹ, nhanh và có gói miễn phí).
- Thêm thư viện mạng (Retrofit hoặc OkHttp) vào `build.gradle.kts` để gọi API.

### 3. Thiết kế System Prompt (Kịch bản cho AI)
Thiết lập System Prompt cho model AI như sau:

> "Bạn là một trợ lý chỉnh sửa ảnh. Người dùng sẽ nói yêu cầu của họ. Nhiệm vụ của bạn là ánh xạ yêu cầu đó sang một trong các bộ lọc sau: BLACK_WHITE, SEPIA, SKETCH, VIGNETTE. 
> Chỉ được phép trả về một cục JSON chuẩn xác với định dạng: `{"action": "apply_filter", "filter_name": "TÊN_FILTER"}`. Không giải thích gì thêm."

### 4. Luồng xử lý Code (Backend Logic)
- **Bắt sự kiện:** User bấm Send -> Lấy text -> Gọi Gemini API trên Background Thread.
- **Xử lý JSON:** Nhận JSON trả về, parse lấy `filter_name`.
- **Thực thi:** Gọi logic của thư viện:
  ```text
  // Ví dụ xử lý JSON trả về là "SEPIA"
  photoEditor.setFilterEffect(PhotoFilter.SEPIA);
  ```

---

## Phase 2: AI Đánh giá & Tinh chỉnh Thông số (Parameter Tuning)

**Mục tiêu:** Nâng cấp AI để không chỉ chọn filter cứng, mà có khả năng phân tích bức ảnh và tinh chỉnh các thông số linh hoạt (Độ sáng, Độ tương phản, Độ bão hòa) dựa theo sắc thái câu từ của người dùng.

### 1. Nâng cấp API (Vision LLM)
- Sử dụng **Gemini Pro Vision** (hoặc model tương đương có khả năng nhìn ảnh).
- Khi người dùng gửi lệnh (VD: "Trời hơi âm u, làm ảnh sáng và tươi hơn một chút đi"), app sẽ nén `Bitmap` hiện tại thành base64 và gửi lên cùng với câu lệnh text.

### 2. Nâng cấp System Prompt
> "Bạn là chuyên gia chỉnh màu ảnh. Dựa vào bức ảnh người dùng cung cấp và yêu cầu của họ, hãy tính toán các thông số điều chỉnh màu sắc phù hợp.
> Trả về JSON định dạng: `{"brightness": float, "contrast": float, "saturation": float}`. Mức bình thường là 1.0. Tăng là > 1.0, giảm là < 1.0."

### 3. Luồng xử lý Code nâng cao
1. **Xây dựng Custom Filter:** Thư viện `PhotoEditor` mặc định có thể không hỗ trợ chỉnh thông số thả nổi. Bạn cần tạo một class `CustomColorMatrixFilter` bên trong folder `processing/filters/` để sử dụng `ColorMatrix`.
2. **Thực thi:** 
   - Nhận JSON từ AI (Ví dụ: `{"brightness": 1.2, "contrast": 1.1, "saturation": 1.3}`).
   - Áp dụng ma trận màu (ColorMatrix) trực tiếp lên `ImageView` hoặc thông qua Custom Filter của PhotoEditor:
   ```text
   ColorMatrix colorMatrix = new ColorMatrix();
   // Logic setBrightness, setContrast, setSaturation...
   ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
   photoEditorView.getSource().setColorFilter(filter);
   ```

---

### Lưu ý Bảo mật & Trải nghiệm (Dành cho Agent)
- **Timeouts:** Gọi API có thể lâu, luôn cài đặt timeout (ví dụ 10s) và báo lỗi thân thiện nếu mạng chậm.
- **Lưu lịch sử:** AI có thể không hiểu đúng ý 100%, hãy đảm bảo tính năng `photoEditor.undo()` hoạt động tốt để người dùng quay lại trạng thái trước khi AI chỉnh sửa.
