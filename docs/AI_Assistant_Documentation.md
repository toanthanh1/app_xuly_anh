# Tài liệu Tính năng AI Assistant (Chat) - App_xhinh_anh

## 1. Tổng quan
Tính năng AI Assistant cho phép người dùng tương tác với trí tuệ nhân tạo (Google Gemini) để nhận tư vấn về chỉnh sửa ảnh hoặc thực hiện các lệnh chỉnh sửa bằng ngôn ngữ tự nhiên. AI được cấu hình để hiểu các bộ lọc và thông số điều chỉnh có sẵn trong ứng dụng.

## 2. Hướng dẫn sử dụng
### Đối với người dùng:
*   **Truy cập:** Mở tính năng Trợ lý AI từ màn hình chính hoặc màn hình biên tập ảnh.
*   **Nhập liệu:** Nhập yêu cầu vào ô chat (ví dụ: "Làm ảnh này trông nghệ thuật hơn", "Tôi muốn da trắng hơn").
*   **Phản hồi:** AI sẽ trả về lời nhắn tư vấn hoặc thực hiện lệnh chỉnh sửa tương ứng (ví dụ: áp dụng bộ lọc "Snow White").

### Luồng xử lý:
1.  Người dùng gửi tin nhắn.
2.  Hệ thống gửi tin nhắn kèm theo **System Prompt** (từ `PromptProvider`) tới Gemini API.
3.  Gemini trả về kết quả dưới dạng text hoặc cấu trúc JSON.
4.  Ứng dụng hiển thị tin nhắn và thực hiện các tác vụ chỉnh sửa ảnh nếu có lệnh hợp lệ.

## 3. Cấu trúc kỹ thuật
Tính năng được đặt trong package: `com.example.app_xhinh_anh.features.ai_assistant`

*   **`ui/AiAssistantActivity.java`**: Quản lý giao diện chat, xử lý sự kiện gửi tin và hiển thị kết quả.
*   **`data/GeminiApiClient.java`**: Lớp giao tiếp trực tiếp với Google Generative AI SDK. Sử dụng model `gemini-2.0-flash` (hoặc bản mới hơn).
*   **`domain/PromptProvider.java`**: Chứa "System Prompt" - tập hợp các quy tắc định hướng để AI hiểu các bộ lọc của ứng dụng (như Snow White, Vivid, Neon Fire...).
*   **`ui/adapter/ChatAdapter.java`**: Adapter hiển thị danh sách tin nhắn trong RecyclerView.

## 4. Bảo trì và Cấu hình

### Cập nhật API Key
API Key không được lưu trực tiếp trong code để bảo mật. Để cập nhật:
1.  Mở file `local.properties` ở thư mục gốc của dự án.
2.  Thêm hoặc sửa dòng: `gemini.api.key=YOUR_API_KEY_HERE`.
3.  Gradle sẽ tự động nạp key này vào `BuildConfig.GEMINI_API_KEY` khi build.

### Thay đổi hành vi AI (System Prompt)
Nếu bạn thêm bộ lọc mới hoặc muốn AI thông minh hơn, hãy chỉnh sửa file `PromptProvider.java`.
*   **Lưu ý:** Luôn yêu cầu AI trả về định dạng nhất quán để code có thể parse được (ví dụ: định dạng JSON cho các hành động `APPLY_FILTER` hoặc `ADJUST`).

### Nâng cấp Model
Trong `GeminiApiClient.java`, bạn có thể thay đổi tên model (ví dụ từ `gemini-1.5-flash` lên các phiên bản cao hơn) tại phần khởi tạo `GenerativeModel`.

### Xử lý lỗi thường gặp
*   **Lỗi 404/Not Found:** Kiểm tra lại tên model và endpoint trong `GeminiApiClient`. Hiện tại đang sử dụng endpoint `v1`.
*   **Lỗi 403/API Key:** Kiểm tra xem API Key trong `local.properties` có còn hạn hay bị giới hạn vùng địa lý không.
*   **Lỗi Unresolved reference 'util':** Đã được khắc phục trong `build.gradle.kts` bằng cách import rõ ràng `java.util.Properties`.

## 5. Mở rộng trong tương lai
*   Tích hợp xử lý ảnh trực tiếp (Multimodal): Gửi ảnh kèm theo prompt để AI phân tích nội dung ảnh trước khi gợi ý bộ lọc.
*   Lưu lịch sử chat: Hiện tại lịch sử chat sẽ mất khi đóng Activity. Có thể sử dụng Room Database để lưu trữ.
