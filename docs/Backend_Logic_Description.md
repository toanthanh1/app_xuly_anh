# Tài liệu Logic Backend cho các nút bấm tại MainActivity

Tài liệu này mô tả logic xử lý (Backend logic) của lớp `ImageActionManager` chịu trách nhiệm quản lý việc chọn ảnh từ thư viện và chụp ảnh từ Camera.

## 1. Thành phần chính
Lớp xử lý chính: `com.example.app_xhinh_anh.utils.ImageActionManager`

### Biến khởi tạo
- `ActivityResultLauncher<String> mGetContent`: Bộ kích hoạt để mở ứng dụng chọn nội dung (thư viện ảnh).
- `ActivityResultLauncher<Uri> mTakePicture`: Bộ kích hoạt để mở ứng dụng Camera.
- `Uri photoUri`: Lưu trữ địa chỉ của file ảnh tạm thời khi chụp từ camera.

---

## 2. Logic xử lý chi tiết

### A. Nút "Chọn ảnh từ thư viện" (`btnPickImage`)
1. **Kích hoạt:** Khi nhấn nút, gọi `mGetContent.launch("image/*")`.
2. **Hành động hệ thống:** Hệ thống Android mở bộ chọn tệp mặc định lọc theo định dạng hình ảnh.
3. **Kết quả trả về:** 
   - Nếu người dùng chọn ảnh: Nhận về một `Uri` (địa chỉ tệp).
   - Gọi hàm `navigateToSecondActivity(uri)` để chuyển sang màn hình 2.

### B. Nút "Chụp ảnh từ camera" (`btnCaptureImage`)
1. **Khởi tạo tệp tạm:** Gọi hàm `createImageUri()` để tạo một file `.jpg` rỗng trong bộ nhớ ứng dụng (`ExternalFilesDir`).
2. **Cấp quyền truy cập:** Sử dụng `FileProvider` để chuyển đổi `File` thành `Uri` hợp lệ, cho phép ứng dụng Camera có quyền ghi dữ liệu vào file đó.
3. **Kích hoạt Camera:** Gọi `mTakePicture.launch(photoUri)`.
4. **Hành động hệ thống:** Mở ứng dụng Camera mặc định của thiết bị.
5. **Kết quả trả về:**
   - Nếu chụp thành công: File tạm đã được điền dữ liệu ảnh.
   - Gọi hàm `navigateToSecondActivity(photoUri)` để hiển thị ảnh vừa chụp ở màn hình 2.

---

## 3. Chuyển tiếp màn hình (`navigateToSecondActivity`)
Hàm này thực hiện các bước:
- Tạo một `Intent` để mở `SecondActivity`.
- Đính kèm dữ liệu ảnh thông qua `intent.putExtra("image_uri", uri.toString())`.
- **Quan trọng:** Thêm cờ `Intent.FLAG_GRANT_READ_URI_PERMISSION` để đảm bảo `SecondActivity` có quyền đọc file ảnh (đặc biệt cần thiết cho ảnh chụp từ camera).

---

## 4. Cấu trúc lưu trữ tệp (Camera)
- **Thư mục lưu trữ:** `/Android/data/com.example.app_xhinh_anh/files/Pictures/`
- **Định dạng tên:** `JPEG_YYYYMMDD_HHMMSS_.jpg`
- **Cơ chế bảo mật:** Sử dụng `androidx.core.content.FileProvider` để tránh lỗi `FileUriExposedException` trên các phiên bản Android mới.
