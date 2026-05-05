# Sequence Diagram: Chat AI Assistant

Tài liệu này mô tả luồng xử lý tin nhắn trong tính năng Trợ lý AI (Chat UI).

## Luồng xử lý tổng quát

```mermaid
sequenceDiagram
    actor User
    participant Activity as AiAssistantActivity
    participant Manager as AiResponseManager
    participant API as GeminiApiClient
    participant Gemini as Gemini AI (Cloud)
    participant Editor as EditorActivity

    User->>Activity: Nhập văn bản & nhấn Gửi
    Activity->>Activity: Hiển thị tin nhắn User lên UI
    
    Note over Activity, Manager: Giai đoạn 1: Kiểm tra từ khóa Local
    Activity->>Manager: handleLocalInput(message)
    
    alt Khớp từ khóa local (Ví dụ: "tăng sáng")
        Manager-->>Activity: onMessage("Đã tăng độ sáng...")
        Manager-->>Activity: onAdjustments(...)
        Activity->>Editor: Trả kết quả (Intent Result)
        Activity->>Activity: Đóng màn hình Chat
    else Không khớp local
        Note over Activity, API: Giai đoạn 2: Gọi Gemini API
        Activity->>Activity: Hiện ProgressBar (Thinking)
        Activity->>API: sendMessage(message)
        API->>Gemini: Gửi Prompt (Content + System Instruction)
        Gemini-->>API: Trả về JSON/Text phản hồi
        API-->>Activity: onSuccess(response)
        
        Note over Activity, Manager: Giai đoạn 3: Phân tích phản hồi
        Activity->>Manager: parseResponse(response)
        Manager->>Manager: Trích xuất JSON từ Markdown (nếu có)
        
        alt Phản hồi là Action (Ví dụ: APPLY_FILTER)
            Manager-->>Activity: onMessage("Đã chọn bộ lọc X")
            Manager-->>Activity: onApplyFilter("FilterName")
            Activity->>Editor: Trả kết quả (Intent Result)
            Activity->>Activity: Đóng màn hình Chat sau 1s
        else Phản hồi là Chat thuần túy
            Manager-->>Activity: onMessage("Lời tư vấn của AI...")
            Activity->>Activity: Hiển thị bong bóng chat AI
            Activity->>Activity: Ẩn ProgressBar
        end
    end
```

## Các thành phần chính

1.  **AiAssistantActivity**: Quản lý giao diện Chat, RecyclerView và điều phối luồng.
2.  **AiResponseManager**: 
    -   `handleLocalInput`: Chứa logic If/Else để phản hồi ngay lập tức cho các lệnh phổ biến mà không tốn phí API.
    -   `parseResponse`: Sử dụng `JSONObject` để bóc tách hành động mà AI yêu cầu thực hiện.
3.  **GeminiApiClient**: Sử dụng Google Generative AI SDK để giao tiếp với model `gemini-pro`.
4.  **Intent Result**: Cơ chế quay về màn hình chỉnh sửa ảnh để thực thi các thay đổi (Filter, Adjustment) mà AI đề xuất.
