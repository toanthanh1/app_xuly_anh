# Roadmap: Triển khai Tính năng Xóa phông AI (Background Removal)

Tài liệu này đóng vai trò là "Implementation Plan" (Lộ trình triển khai) dành cho các AI Agent (như Cline, Cursor, Roo Code, v.v.) để tích hợp tính năng Xóa phông bằng AI vào ứng dụng App_xhinh_anh.

## 1. Công nghệ đề xuất
Để thực hiện việc xóa nền trực tiếp trên thiết bị (On-device) nhanh chóng mà không cần gọi API từ server, đề xuất sử dụng **Google ML Kit (Subject Segmentation)** hoặc **Selfie Segmentation**.
- **Ưu điểm:** Miễn phí, chạy offline, bảo mật dữ liệu, dễ tích hợp trên Android.

---

## 2. Các giai đoạn triển khai (Phases)

### Giai đoạn 1: Cài đặt Dependency (Dependencies Setup)
1. Mở file `app/build.gradle.kts`.
2. Thêm thư viện ML Kit vào block `dependencies`:
   ```text
   // Dùng Subject Segmentation (Tách chủ thể tổng quát)
   implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")
   // HOẶC Selfie Segmentation (Chỉ chuyên tách người)
   // implementation("com.google.mlkit:vision-interfaces:16.2.0")
   ```
3. Sync lại Gradle.

### Giai đoạn 2: Triển khai Core AI Logic (Xử lý nền tảng)
1. Tạo class `BackgroundRemoverAi` tại `app/src/main/java/com/example/app_xhinh_anh/processing/ai/`.
2. Yêu cầu class này `implements AiProcessor` (đã có sẵn bộ khung).
3. Triển khai hàm `process(Bitmap input, AiCallback callback)`:
   - Chuyển đổi `Bitmap` input sang định dạng `InputImage` của ML Kit.
   - Cấu hình `SubjectSegmenter` (hoặc `SelfieSegmenter`).
   - Đưa `InputImage` vào model để xử lý.
   - Từ kết quả (`SubjectSegmentationResult` - bao gồm mask/ảnh mặt nạ), tiến hành lặp qua các pixel hoặc dùng `Canvas`/`PorterDuff.Mode.CLEAR` để tạo ra một `Bitmap` mới có nền trong suốt (Transparent Background).
   - Trả kết quả `Bitmap` mới qua `callback.onSuccess()`. Đảm bảo handle Exception qua `callback.onError()`.

### Giai đoạn 3: Tích hợp Giao diện (UI Integration)
1. Mở `activity_editor.xml` (hoặc layout tương ứng chứa các công cụ chỉnh sửa).
2. Thêm một nút (Button/ImageButton) với icon "Xóa nền" (Magic wand / Background remove icon).
3. Mở `EditorActivity.java`:
   - Bắt sự kiện click cho nút "Xóa nền".
   - Hiển thị một `ProgressDialog` hoặc `ProgressBar` (Loading) vì xử lý AI tốn vài giây.
   - Khởi tạo `BackgroundRemoverAi` và gọi hàm `process(...)`.
   - Nhận kết quả ở UI Thread:
     - Ẩn Loading.
     - Cập nhật ảnh kết quả (nền trong suốt) lên màn hình (`photoEditor` hoặc `ImageView` chính).

### Giai đoạn 4: Tối ưu hóa và Xử lý lỗi (Optimization & Error Handling)
- **Out of Memory (OOM):** Quản lý chặt chẽ vòng đời của `Bitmap`. Sử dụng `bitmap.recycle()` cho các ảnh mask trung gian nếu không còn dùng đến.
- **Background Thread:** ML Kit tự động chạy trên background, nhưng việc thao tác tạo Bitmap từ Mask có thể nặng. Phải đảm bảo thao tác xử lý pixel nằm trên background thread (ví dụ: dùng `ExecutorService`), và chỉ gọi `callback.onSuccess` trên Main Thread (dùng `Handler(Looper.getMainLooper())`).
- **Lỗi không có chủ thể:** Nếu ảnh cảnh vật không có chủ thể rõ ràng, handle thông báo "Không tìm thấy chủ thể để xóa phông" cho người dùng.

---

## 3. Checklist cho Agent (Dành cho Agent khi bắt tay vào làm)
- [ ] Kiểm tra và nâng cấp cấu hình máy ảo (Heap size) nếu cần khi test.
- [ ] Tuân thủ chặt chẽ interface `AiProcessor.java`.
- [ ] Không chèn logic AI trực tiếp vào `EditorActivity`.
- [ ] Viết comment đầy đủ cho thuật toán xử lý mask pixel (phần khó nhất).
