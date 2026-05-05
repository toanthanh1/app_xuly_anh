# Tài liệu Kỹ thuật: Hệ thống Hoàn tác (Undo/Redo)

Hệ thống Undo/Redo trong ứng dụng được thiết kế theo cơ chế **Hybrid Stack**, quản lý song song hai loại dữ liệu: Đối tượng đồ họa (View-based) và Dữ liệu điểm ảnh (Bitmap-based).

## 1. Cơ chế hoạt động chính

Hệ thống sử dụng các Stack để lưu trữ lịch sử thay đổi:
- **`undoOpStack` / `redoOpStack`**: Lưu trữ *loại* thao tác vừa thực hiện (`BITMAP` hoặc `VIEW`). Đây là "kim chỉ nam" để hệ thống biết cần khôi phục cái gì.
- **`undoBitmapStack` / `redoBitmapStack`**: Lưu trữ các bản sao `Bitmap` tại từng thời điểm thay đổi.
- **`photoEditor` History**: Tự quản lý lịch sử các đối tượng như Sticker, Text, Brush.

## 2. Phân loại Thao tác (OpType)

| Loại | Thao tác áp dụng | Cách quản lý |
| :--- | :--- | :--- |
| **BITMAP** | Bộ lọc (Filter), Chỉnh thông số (Adjust), Tẩy AI, Cắt/Lật ảnh. | Chụp lại toàn bộ `Bitmap` và đưa vào Stack. |
| **VIEW** | Thêm văn bản (Text), Dán Sticker, Vẽ tay (Brush). | Sử dụng hàm `undo()` / `redo()` có sẵn của thư viện `PhotoEditor`. |

## 3. Quy trình thực hiện (Workflow)

### Khi thực hiện một hành động mới:
1. **Lưu trạng thái**: Trước khi thay đổi `Bitmap`, hàm `saveBitmapState()` được gọi để copy `Bitmap` hiện tại vào `undoBitmapStack`.
2. **Ghi nhận loại**: Thêm `OpType.BITMAP` hoặc `OpType.VIEW` vào `undoOpStack`.
3. **Làm sạch Redo**: Xóa toàn bộ dữ liệu trong các Redo Stack (vì một chuỗi lịch sử mới đã bắt đầu).

### Khi nhấn Undo (Hoàn tác):
1. Lấy loại thao tác cuối cùng từ `undoOpStack`.
2. Nếu là **BITMAP**:
   - Chụp `Bitmap` hiện tại bỏ vào `redoBitmapStack`.
   - Lấy `Bitmap` cũ nhất từ `undoBitmapStack` và hiển thị lại.
3. Nếu là **VIEW**:
   - Gọi `photoEditor.undo()`.
4. Đẩy loại thao tác đó sang `redoOpStack`.

## 4. Các bước triển khai trong Code

Để đảm bảo tính ổn định, hệ thống tuân thủ 4 bước nghiêm ngặt:

1. **Copy Bitmap**: Luôn sử dụng `Bitmap.copy()` để tạo bản sao vật lý, tránh việc stack chỉ lưu tham chiếu đến cùng một đối tượng ảnh đang thay đổi.
2. **Quản lý bộ nhớ**: Do lưu trữ nhiều `Bitmap` trong bộ nhớ, các ảnh được đưa vào Stack thường là bản preview hoặc ảnh đã tối ưu dung lượng để tránh lỗi `OutOfMemory`.
3. **Phân luồng**: Thao tác cập nhật UI sau khi Undo/Redo phải luôn chạy trên `MainThread`.
4. **Đồng bộ hóa**: Khi thực hiện Undo loại `BITMAP`, hệ thống tự động xóa các Filter đang áp dụng trên View để tránh hiện tượng "chồng Filter" lên ảnh cũ.

## 5. Giới hạn hệ thống
- **Số bước lưu trữ**: Hiện tại hệ thống cho phép Undo/Redo lên đến ~10-15 bước tùy thuộc vào cấu hình bộ nhớ thiết bị.
- **Độ ưu tiên**: Thao tác `BITMAP` được ưu tiên khôi phục trạng thái hiển thị của `ImageView` gốc trước khi thực hiện các lệnh khác.
