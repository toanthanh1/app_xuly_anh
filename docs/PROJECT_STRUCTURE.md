# Tài liệu Cấu trúc Project - App Chỉnh Sửa Hình Ảnh

Tài liệu này hướng dẫn về cách tổ chức thư mục (folder structure) trong dự án, giúp các developer và AI agent hiểu rõ ngữ cảnh và vị trí đặt code phù hợp.

## 1. Nguyên tắc tổ chức
Dự án được tổ chức theo hướng **Feature-based UI** kết hợp với tách biệt logic xử lý (Processing) và tiện ích (Utils).

## 2. Sơ đồ cây thư mục (Project Tree)

Dưới đây là cấu trúc trực quan của các thư mục quan trọng trong project:

```text
app/src/main/
├── java/com/example/app_xhinh_anh/
│   ├── ui/                         # Giao diện người dùng (Activities, ViewModels)
│   │   ├── main/                   # Package cho màn hình chính
│   │   │   ├── MainActivity.java
│   │   │   └── ImageActionManager.java
│   │   └── editor/                 # Package cho màn hình chỉnh sửa
│   │       └── EditorActivity.java
│   ├── utils/                      # Các lớp tiện ích dùng chung (Bitmap, File, Permissions)
│   ├── processing/                 # Logic xử lý ảnh (Filters, Tools, AI)
│   │   ├── filters/                # Các bộ lọc màu
│   │   ├── tools/                  # Công cụ: Crop, Rotate, Draw...
│   │   └── ai/                     # Xử lý AI (ML Kit, Background Removal)
│   │       ├── AiProcessor.java    # Interface chung cho các bộ xử lý AI
│   │       └── BackgroundRemoverAi.java # Triển khai xóa nền AI
│   └── data/                       # [Đề xuất] Quản lý dữ liệu và Repository
│
└── res/layout/                     # Các tệp giao diện XML
    ├── activity_main.xml           # Layout màn hình chính
    ├── activity_editor.xml         # Layout màn hình chỉnh sửa
    └── component_image_buttons.xml # Component dùng chung cho các button chọn ảnh
```

## 3. Chi tiết các thư mục (Packages)

### `com.example.app_xhinh_anh.ui`
Chứa các thành phần liên quan đến giao diện người dùng.
*   **`ui.main`**:
    *   `MainActivity.java`: Màn hình chính nơi người dùng chọn hoặc chụp ảnh.
    *   `ImageActionManager.java`: Quản lý logic chọn ảnh từ Gallery hoặc Camera.
*   **`ui.editor`**:
    *   `EditorActivity.java`: Nơi thực hiện các thao tác chỉnh sửa (Filter, Crop, v.v.).

### `com.example.app_xhinh_anh.utils`
Chứa các lớp tiện ích (Helper/Utility classes) dùng chung.
*   **Các file nên có:** `BitmapUtils.java`, `FileUtils.java`, `PermissionUtils.java`.

### `com.example.app_xhinh_anh.processing`
Nơi chứa logic xử lý hình ảnh thực tế - tách biệt hoàn toàn khỏi Activity để dễ bảo trì.
*   **`processing.ai`**: Chứa các lớp xử lý trí tuệ nhân tạo (AI).
    *   `AiProcessor.java`: Interface định nghĩa cách các bộ xử lý AI hoạt động.
    *   `BackgroundRemoverAi.java`: Tính năng **Xóa phông nền** - Sử dụng Google ML Kit để tự động tách chủ thể và xóa nền ảnh (Background Removal).
*   **`processing.filters`**: Quản lý các bộ lọc màu (Lut, ColorMatrix).
*   **`processing.tools`**: Các công cụ biến đổi hình học (Crop, Flip).

### `com.example.app_xhinh_anh.data` (Cần tạo thêm khi phát triển)
Quản lý việc lưu trữ ảnh vào bộ nhớ máy hoặc xử lý dữ liệu bền vững.

## 4. Hướng dẫn sử dụng cho Agent/Developer

1.  **Khi thêm tính năng mới:** Nếu là tính năng giao diện (màn hình mới), hãy tạo package con trong `ui`. Nếu là thuật toán xử lý ảnh, hãy đặt trong `processing`.
2.  **Tránh "Fat Activity":** Đừng viết toàn bộ logic xử lý ảnh trong `EditorActivity`. Hãy viết logic đó trong một class riêng ở `processing` và gọi nó từ Activity.
3.  **Quản lý Resource:** Các file layout XML phải được đặt tên theo quy tắc `activity_xxx.xml` hoặc `component_xxx.xml` và phải cập nhật `tools:context` chính xác.

## 5. Thư viện hỗ trợ (Third-party Libraries)

Dự án sử dụng các thư viện sau để tối ưu hóa việc xử lý ảnh:

*   **Glide (com.github.bumptech.glide):** Thư viện tải và hiển thị hình ảnh mạnh mẽ, giúp quản lý bộ nhớ và cache ảnh hiệu quả.
*   **uCrop (com.github.yalantis:ucrop):** Cung cấp giao diện và logic cắt (crop) ảnh chuyên nghiệp, hỗ trợ xoay và thay đổi tỉ lệ khung hình.
*   **PhotoEditor (com.burhanrashid52:photoeditor):** Thư viện hỗ trợ các tính năng chỉnh sửa nâng cao như: vẽ (drawing), thêm văn bản (text), dán nhãn (stickers) và các bộ lọc màu cơ bản trên ảnh.
*   **ML Kit Subject Segmentation (google.android.gms:play-services-mlkit-subject-segmentation):** Cung cấp khả năng tách chủ thể (subject) ra khỏi nền bằng AI ngay trên thiết bị, hỗ trợ tính năng "Xóa nền AI".

---
*Ghi chú: Cấu trúc này giúp dự án sạch sẽ (Clean Code) và dễ dàng mở rộng khi thêm nhiều bộ lọc phức tạp.*
