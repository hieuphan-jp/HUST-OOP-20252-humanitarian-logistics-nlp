package com.disaster.analysis.domain.contract.preprocessing;

/**
 * Bản hợp đồng định nghĩa công cụ "tẩy rửa" và tiền xử lý văn bản thô.
 * Công cụ này giúp loại bỏ emoji, icon, ký tự lạ, hoặc chuẩn hóa font chữ trước khi đưa vào phân tích.
 */
public interface TextPreprocessor {

    /**
     * Làm sạch một đoạn văn bản thô.
     *
     * @param rawContent Văn bản chứa rác, emoji, ký tự HTML...
     * @return Văn bản đã được làm sạch, sẵn sàng cho việc phân tích từ vựng.
     */
    String preprocess(String rawContent);
}