package com.disaster.analysis.domain.contract.ai;

import com.disaster.analysis.domain.exception.AIClientException;

/**
 * Bản hợp đồng (Contract) định nghĩa cách hệ thống giao tiếp với các dịch vụ Trí tuệ nhân tạo (LLM).
 * Các lớp thực thi ở tầng Infrastructure (như OpenAIClient, GeminiClient) phải tuân thủ hợp đồng này.
 */
public interface AIClient {

    /**
     * Gửi dữ liệu đã được tổng hợp (prompt) lên AI và nhận về bản báo cáo.
     *
     * @param prompt Chuỗi văn bản chứa lệnh và dữ liệu.
     * @return Văn bản báo cáo tổng hợp do AI sinh ra.
     * @throws AIClientException Nếu có lỗi mạng, sai API Key hoặc hết hạn mức.
     */
    String generateSummary(String prompt) throws AIClientException;

    /**
     * Lấy tên của mô hình AI đang được sử dụng.
     * @return Tên mô hình (Ví dụ: "gpt-4", "gemini-pro").
     */
    String getModelName();

    /**
     * Kiểm tra trạng thái sẵn sàng của AI (Ví dụ: API Key đã được cấu hình chưa).
     * @return true nếu sẵn sàng hoạt động.
     */
    boolean isAvailable();
}