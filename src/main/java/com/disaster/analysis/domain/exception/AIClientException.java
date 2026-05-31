package com.disaster.analysis.domain.exception;

/**
 * Ngoại lệ được ném ra khi có lỗi trong quá trình giao tiếp với các dịch vụ AI bên ngoài
 * (ví dụ: OpenAI, Gemini).
 * Có thể là do lỗi mạng, sai API Key, hoặc giới hạn request (Rate Limit).
 */
public class AIClientException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông báo lỗi.
     * * @param message Chi tiết về lỗi (Ví dụ: "Hết hạn mức API Key").
     */
    public AIClientException(String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ bọc theo một lỗi gốc từ thư viện kết nối mạng.
     * * @param message Chi tiết về lỗi.
     * @param cause   Lỗi gốc gây ra vấn đề (Ví dụ: IOException do mất mạng).
     */
    public AIClientException(String message, Throwable cause) {
        super(message, cause);
    }
}