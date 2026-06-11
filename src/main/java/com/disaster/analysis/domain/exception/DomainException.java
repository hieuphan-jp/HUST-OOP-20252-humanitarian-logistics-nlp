package com.disaster.analysis.domain.exception;

/**
 * Lớp ngoại lệ gốc (Base Exception) cho toàn bộ lỗi nghiệp vụ của hệ thống.
 * Kế thừa RuntimeException để code không bị ép buộc phải try-catch ở mọi nơi.
 */
    public class DomainException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ chỉ với câu thông báo lỗi.
     * Dùng khi dữ liệu không hợp lệ (Ví dụ: "Dự án không tồn tại").
     *
     * @param message Thông báo chi tiết về lỗi.
     */
    public DomainException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ bọc theo một lỗi gốc (Cause).
     * Dùng khi muốn ném lỗi lên tầng trên nhưng vẫn giữ lại dấu vết của lỗi kỹ thuật bên dưới.
     *
     * @param message Thông báo chi tiết về lỗi.
     * @param cause   Ngoại lệ gốc (Ví dụ: SQLException).
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}