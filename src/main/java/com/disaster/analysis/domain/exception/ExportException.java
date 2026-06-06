package com.disaster.analysis.domain.exception;

/**
 * Ngoại lệ được ném ra khi hệ thống gặp lỗi trong quá trình xuất dữ liệu ra tệp tin.
 * (Ví dụ: Xuất báo cáo CSV, Excel, PDF hoặc file Text).
 */
public class ExportException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     * * @param message Chi tiết về lỗi (Ví dụ: "Không có quyền ghi đè lên file report.xlsx").
     */
    public ExportException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ bọc theo lỗi gốc từ thao tác đọc/ghi file.
     * * @param message Chi tiết về lỗi.
     * @param cause   Lỗi gốc (Thường là java.io.IOException).
     */
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}