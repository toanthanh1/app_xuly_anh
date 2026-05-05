# Tài liệu Kỹ thuật: Phối hợp Hàm và Thư viện trong Hệ thống AI

Tài liệu này chi tiết hóa cách các thư viện phần mềm và các hàm chức năng phối hợp với nhau để thực thi các tính năng AI trong ứng dụng.

---

## 1. Hệ thống Trợ lý AI (AI Chat)

### Các thành phần phối hợp:
*   **Thư viện**: `com.google.ai.client.generativeai` (Gemini SDK), `Android Intent System`.
*   **Hàm then chốt**: 
    - `AiResponseManager.handleLocalInput()`: Lọc lệnh cục bộ.
    - `GeminiApiClient.sendMessage()`: Giao tiếp với Cloud AI.
    - `EditorActivity.applyAiAdjustment()`: Thực thi lệnh thay đổi thông số.

### Luồng phối hợp:
1.  **Giao tiếp (Interaction)**: `AiAssistantActivity` nhận văn bản. Nó gọi `AiResponseManager` để phân loại. 
2.  **Xử lý (Processing)**: Nếu lệnh là "Tăng sáng", `AiResponseManager` tạo đối tượng `Adjustment`. Nếu lệnh phức tạp, nó gọi `GeminiApiClient` để nhận về chuỗi JSON.
3.  **Chuyển giao (Handover)**: Sau khi có lệnh JSON, `AiAssistantActivity` đóng gói vào mảng `adjust_props` và `adjust_values` bên trong một `Intent`.
4.  **Thực thi (Execution)**: `EditorActivity` nhận Intent qua `onActivityResult`, bóc tách mảng và chạy vòng lặp gọi `applyAiAdjustment()`. Tại đây, các biến như `brightnessValue` được cập nhật và gọi `applyColorAdjustments()` để render lại ảnh.

---

## 2. Hệ thống Xóa phông nền (Background Removal)

### Các thành phần phối hợp:
*   **Thư viện**: `com.google.mlkit:subject-segmentation`.
*   **Hàm then chốt**: 
    - `SubjectSegmenter.process(InputImage)`: Kích hoạt model AI quét ảnh.
    - `SubjectSegmentationResult.getForegroundBitmap()`: Trích xuất đối tượng.
    - `EditorActivity.saveBitmapState()`: Lưu trạng thái trước khi xóa.

### Luồng phối hợp:
1.  **Chuẩn bị (Preparation)**: Khi người dùng nhấn "Xóa nền", `EditorActivity` gọi `saveBitmapState()` để bảo vệ ảnh gốc vào `undoBitmapStack`.
2.  **Phân tách (Segmentation)**: Ảnh được chuyển thành `InputImage`. Thư viện **ML Kit** nhận dữ liệu này, model AI thực hiện phân tích các lớp Pixel để xác định đâu là người/vật.
3.  **Trích xuất (Extraction)**: Hàm `process().addOnSuccessListener` nhận kết quả. Ứng dụng gọi `getForegroundBitmap()` để lấy lớp ảnh đã tách nền (vùng nền có Alpha = 0).
4.  **Cập nhật (Update)**: Kết quả được đưa vào `PhotoEditorView.getSource()` và gọi `invalidate()` để vẽ lại giao diện với ảnh đã sạch nền.

---

## 3. Hệ thống Tẩy vật thể (Smart Eraser)

### Các thành phần phối hợp:
*   **Thư viện**: `android.graphics` (Bitmap, Canvas, Paint).
*   **Hàm then chốt**: 
    - `MaskOverlayView.exportMaskForImage()`: Xuất mặt nạ vùng tô.
    - `Inpainter.inpaint(Bitmap, Bitmap)`: Thuật toán nội suy lõi.
    - `EditorActivity.applyAiErase()`: Điều phối luồng xử lý.

### Luồng phối hợp:
1.  **Thu thập dữ liệu (Data Collection)**: Người dùng tô lên `MaskOverlayView`. Khi nhấn xác nhận, hàm `exportMaskForImage()` sẽ tính toán tỉ lệ giữa màn hình và ảnh gốc để tạo ra một `maskBitmap` chính xác 1:1.
2.  **Xử lý thuật toán (Algorithm)**: `EditorActivity` gửi cả ảnh gốc và ảnh mặt nạ vào hàm `Inpainter.inpaint()`. 
3.  **Nội suy (Interpolation)**: Bên trong `Inpainter`, một vòng lặp tìm các pixel biên của vùng tô. Nó sử dụng các hàm của thư viện `android.graphics.Color` để lấy màu từ vùng không bị tô, tính toán trung bình cộng (Gaussian) và đắp vào vùng trống.
4.  **Hòa trộn (Blending)**: Sau khi lấp đầy, thuật toán sử dụng `Canvas` để vẽ lại vùng biên với chế độ làm mờ (Blur), giúp vùng tẩy không bị sắc cạnh so với ảnh gốc.

---

## 4. Bảng tổng hợp sự phối hợp

| Tính năng | Điểm kích hoạt (UI) | Thư viện điều phối | Hàm xử lý lõi | Kết quả đầu ra |
| :--- | :--- | :--- | :--- | :--- |
| **AI Chat** | Send Button | Gemini SDK | `applyAiAdjustment` | Cấu hình tham số ColorMatrix |
| **Xóa nền** | Remove BG Button | ML Kit | `getForegroundBitmap` | Bitmap đã tách lớp Alpha |
| **Tẩy AI** | Erase Checkmark | Android Graphics | `Inpainter.inpaint` | Bitmap nội suy vùng trống |

### Nguyên lý phối hợp chung:
Tất cả các tính năng đều tuân thủ quy trình: **Lưu trạng thái (Save State) -> Xử lý (Process) -> Cập nhật View (Refresh UI)**. Việc sử dụng các thư viện chuyên biệt (ML Kit cho nhận diện, Gemini cho ngôn ngữ) giúp giảm tải cho thiết bị, trong khi các hàm cục bộ (Inpainter, Editor) đảm bảo tốc độ phản hồi ngay lập tức cho người dùng.
