# Sequence Diagram: Tẩy AI (Smart Eraser)

Dưới đây là luồng xử lý chi tiết từ khi người dùng bắt đầu tô chọn vùng xóa cho đến khi nhận kết quả ảnh đã xử lý.

```mermaid
sequenceDiagram
    actor User
    participant Activity as EditorActivity
    participant Overlay as MaskOverlayView
    participant Inpainter as Inpainter (Algorithm)
    participant View as PhotoEditorView

    User->>Activity: Nhấn nút "Tẩy AI"
    Activity->>Activity: Hiển thị Panel Smart Eraser
    User->>Activity: Chọn icon "Tẩy AI" (SmartEraserAi)
    
    Activity->>Overlay: openMaskPanel()
    Overlay->>Overlay: Kích hoạt chế độ vẽ (Stroke)
    
    User->>Overlay: Dùng tay tô lên vật thể cần xóa
    Overlay->>Overlay: Vẽ vào maskBitmap (màu đỏ hiển thị)
    
    User->>Activity: Nhấn nút "Check" (Xác nhận xóa)
    Activity->>Activity: applyAiErase()
    Activity->>Activity: Hiện Loading Dialog ("Đang xử lý...")
    
    Activity->>Overlay: exportMaskForImage(rect, width, height)
    Overlay-->>Activity: Trả về Mask Bitmap (khớp tỉ lệ ảnh gốc)
    
    Note over Activity, Inpainter: Xử lý chạy dưới Thread ngầm (Background)
    Activity->>Inpainter: inpaint(srcBitmap, maskBitmap)
    
    activate Inpainter
    Inpainter->>Inpainter: Downscale (về 512px)
    Inpainter->>Inpainter: Onion-peel Fill (điền vùng trống)
    Inpainter->>Inpainter: Smoothing & Blending
    Inpainter-->>Activity: Trả về Result Bitmap
    deactivate Inpainter
    
    Activity->>View: Cập nhật ảnh mới lên màn hình
    Activity->>Activity: Lưu trạng thái vào Undo Stack
    Activity->>Activity: Ẩn Loading & Đóng Mask Panel
    Activity->>Activity: Toast "Đã tẩy xong"
```

## Giải thích các bước quan trọng:

1.  **Chuyển đổi tọa độ**: Vì ảnh hiển thị trong `PhotoEditorView` có thể bị co giãn (Letterbox/FitCenter), `EditorActivity` phải tính toán `RectF` chính xác để trích xuất Mask từ `MaskOverlayView` sao cho khớp từng pixel với ảnh gốc.
2.  **Thread ngầm**: Việc xử lý `Inpainter` tốn nhiều CPU nên được thực hiện trong một `Thread` riêng để không làm treo giao diện (UI Thread).
3.  **Hòa trộn (Blending)**: Bước cuối cùng trong `Inpainter` cực kỳ quan trọng; nó chỉ lấy vùng đã sửa đè lên vùng Mask của ảnh gốc, giúp giữ nguyên 100% độ nét của những vùng không bị tác động.
