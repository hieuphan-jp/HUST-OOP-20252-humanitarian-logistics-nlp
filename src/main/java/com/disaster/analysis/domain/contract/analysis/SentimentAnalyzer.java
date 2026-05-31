package com.disaster.analysis.domain.contract.analysis;

import com.disaster.analysis.domain.model.enums.Sentiment;

/**
 * Bản hợp đồng định nghĩa bộ phân tích cảm xúc (Tích cực, Tiêu cực, Trung lập).
 */
public interface SentimentAnalyzer {

    /**
     * Khởi tạo bộ phân tích (Ví dụ: Tải các bộ từ điển hoặc nạp AI model vào RAM).
     * Hàm này nên được gọi một lần duy nhất lúc khởi động app.
     */
    void initialize();

    /**
     * Đọc một đoạn văn bản và chấm điểm cảm xúc cho nó.
     *
     * @param text Nội dung bài viết hoặc bình luận.
     * @return Kết quả cảm xúc {@link Sentiment}.
     */
    Sentiment analyzeSentiment(String text);

    /**
     * Kiểm tra xem bộ phân tích đã được khởi tạo thành công chưa.
     */
    boolean isInitialized();
}