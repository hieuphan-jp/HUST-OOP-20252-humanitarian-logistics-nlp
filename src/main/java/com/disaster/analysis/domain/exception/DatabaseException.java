package com.disaster.analysis.domain.exception;

/**
 * Ngoại lệ được ném ra khi xảy ra các vấn đề liên quan đến Cơ sở dữ liệu (Database).
 * Ví dụ: Mất kết nối tới SQL Server, lỗi cú pháp SQL, hoặc vi phạm khóa ngoại.
 */
public class DatabaseException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     * * @param message Chi tiết về lỗi (Ví dụ: "Không thể kết nối đến SQL Server ở cổng 1433").
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ bọc theo lỗi gốc (thường là SQLException).
     * Điều này giúp tầng UI không bị dính dáng trực tiếp đến thư viện SQL của Java.
     * * @param message Chi tiết về lỗi.
     * @param cause   Lỗi gốc (Thường là java.sql.SQLException).
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}